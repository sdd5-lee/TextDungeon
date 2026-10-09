package com.textdungeon.ai;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.textdungeon.ai.backend.LlmBackend;
import com.textdungeon.ai.backend.LlmCallback;
import com.textdungeon.ai.backend.ResponseFormat;
import com.textdungeon.event.GameEvent;
import com.textdungeon.event.ShopEvent;
import com.textdungeon.model.Item;
import com.textdungeon.model.Stat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 혼돈의 주사위.
 *
 * 예전 방식: 이벤트 전체를 JSON으로 보내고, AI가 이벤트 전체를 다시 써서 돌려줌 → BattleEvent로 통째 역직렬화
 * 지금 방식: AI는 "새 선택지 1개 + 보상"만 줄 단위로 출력 → 코드가 기존 이벤트 객체에 직접 붙임
 *
 * 출력량이 크게 줄어서 온디바이스 소형 모델로도 처리할 수 있는 크기가 되고,
 * 원래 이벤트의 클래스(GameEvent/BattleEvent)도 그대로 유지된다.
 */
public class ChaosDice {
    private static final String TAG = "ChaosDice";

    private final LlmBackend backend;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ChaosDice(LlmBackend backend) {
        this.backend = backend;
    }

    public void roll(int floor, Stat stat, List<Item> itemList, GameEvent currentEvent, AiCallback callback) {
        // 상점은 어떤 선택지를 눌러도 상점이 열리므로 선택지를 추가해도 의미가 없다
        if (currentEvent instanceof ShopEvent) {
            postError(callback, "상점 이벤트에는 주사위를 사용할 수 없습니다.");
            return;
        }
        if (currentEvent == null || !currentEvent.canAppendChoice()) {
            postError(callback, "선택지와 보상 개수가 맞지 않는 이벤트입니다.");
            return;
        }

        Set<String> validItemIds = new HashSet<>();
        for (Item item : itemList) {
            if (item != null && item.getId() != null) validItemIds.add(item.getId());
        }

        String prompt = buildPrompt(floor, stat, itemList, currentEvent);

        backend.generate(prompt, ResponseFormat.TEXT, new LlmCallback() {
            @Override
            public void onResult(String text) {
                ChoiceDelta parsed;
                try {
                    parsed = ChoiceLineParser.parse(text, validItemIds);
                } catch (ChoiceLineParser.ParseException e) {
                    Log.e(TAG, "파싱 실패(" + e.getMessage() + ") 원문:\n" + text);
                    postError(callback, "혼돈의 결말 해석 실패: " + e.getMessage());
                    return;
                }
                // 파서 상한(±9999)은 형식 방어용일 뿐이라, 실제 게임 수치는 층별 상한으로 다시 자른다
                final ChoiceDelta delta = parsed.withClampedStats(floor);
                // 이벤트 객체는 UI가 읽고 있으므로 수정은 메인 스레드에서
                mainHandler.post(() -> {
                    try {
                        currentEvent.addChoiceWithReward(delta.getChoiceText(), delta.toReward());
                    } catch (IllegalStateException e) {
                        Log.e(TAG, "선택지 추가 실패", e);
                        callback.onError(e.getMessage());
                        return;
                    }
                    Log.d(TAG, "혼돈의 주사위 성공: " + delta.getChoiceText());
                    com.textdungeon.ai.backend.GeminiBackend.logRaw("해석된 주사위 선택지",
                            "선택지=" + delta.getChoiceText() + " / 보상설명=" + delta.getRewardDescription()
                                    + " / 아이템=" + delta.getItemId() + " / 스탯 " + delta.getStats().size() + "개");
                    callback.onSuccess(currentEvent);
                });
            }

            @Override
            public void onError(String errorMessage) {
                postError(callback, errorMessage);
            }
        });
    }

    private void postError(AiCallback callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }

    /**
     * 이벤트 전체 JSON 대신 필요한 정보만 짧게 넣는다 (입력 토큰도 줄어든다).
     */
    private static String buildPrompt(int floor, Stat stat, List<Item> itemList, GameEvent event) {
        String items = itemList.stream()
                .filter(i -> i != null && i.getId() != null)
                .map(i -> i.getId() + ":" + i.getName())
                .collect(Collectors.joining(", "));

        StringBuilder choices = new StringBuilder();
        List<String> existing = event.getChoices();
        if (existing != null) {
            for (int i = 0; i < existing.size(); i++) {
                choices.append(i + 1).append(") ").append(existing.get(i)).append('\n');
            }
        }

        return "[혼돈의 신 | " + floor + "층]\n"
                + "플레이어: 힘 " + stat.getStrength()
                + ", 민첩 " + stat.getAgility()
                + ", 체력 " + stat.getHealth()
                + ", 지혜 " + stat.getWisdom()
                + ", HP " + stat.getHp() + "/" + stat.getMaxHp()
                + ", 골드 " + stat.getGold() + "\n"
                + "아이템 목록(id:이름): " + items + "\n\n"
                + "현재 이벤트: " + event.getName() + "\n"
                + "상황: " + event.getDescription() + "\n"
                + "기존 선택지:\n" + choices + "\n"
                + "이 상황에 어울리는 새로운 선택지 1개를 만들어라. 결과는 최악 또는 최상 중 무작위로 정하라.\n"
                + "아래 형식 그대로, 다른 말 없이 출력하라.\n\n"
                + "선택지: (20자 내외의 행동)\n"
                + "보상설명: (TRPG 서사체로 결과를 묘사하고 수치를 그대로 적는다)\n"
                + "아이템: (아이템 목록의 id 하나, 없으면 없음)\n"
                + "스탯: 종류=수치\n\n"
                + "규칙:\n"
                + "- 스탯 종류는 힘, 민첩, 체력, 지혜, 경험치, 데미지, 회복 중에서만 고른다. 스탯 줄은 0~3개.\n"
                + "- 체력이 깎이면 데미지=양수, 체력 회복은 회복=양수로 적는다.\n"
                + "- 보상설명에 적은 수치와 스탯 줄의 수치를 일치시킨다.";
    }
}