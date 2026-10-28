package com.textdungeon.ai;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.textdungeon.ai.backend.LlmBackend;
import com.textdungeon.ai.backend.LlmCallback;
import com.textdungeon.ai.backend.ResponseFormat;
import com.textdungeon.ai.backend.ResponseValidator;
import com.textdungeon.event.GameEvent;
import com.textdungeon.event.ShopEvent;
import com.textdungeon.model.Item;
import com.textdungeon.model.Stat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
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
    /** 프롬프트에 넣는 아이템 후보 수 */
    static final int ITEM_CANDIDATES = 5;
    // 형식을 보여 주는 예시. 모델이 이 선택지를 그대로 베끼면 실패로 처리한다
    static final String EXAMPLE_CHOICE = "제단의 촛불을 맨손으로 끈다";
    // HP 변화는 "HP-12"처럼 쓰게 하고 파서가 데미지/회복으로 바꾼다.
    // "데미지"를 적에게 준 피해로, "체력"을 HP로 쓰는 경우가 많아서 두 단어는 모델에게 보여 주지 않는다
    // 보상설명에는 수치를 쓰지 않게 한다. 실제 적용량은 코드가 끝에 붙인다 (Reward.summary)
    private static final String EXAMPLE_DESCRIPTION = "꺼진 심지에서 검은 연기가 피어올라 팔을 태우지만, 근육이 터질 듯 부풀어 오른다.";
    private static final String EXAMPLE_STATS = "힘+5, HP-12";

    private final LlmBackend backend;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ChaosDice(LlmBackend backend) {
        this.backend = backend;
    }

    /**
     * @param rewardMultiplier 난이도 보상 배수, expMultiplier 특성 경험치 배수.
     *                         보상설명 끝에 붙이는 요약을 실제 적용량으로 만드는 데 쓴다 (Reward.summary)
     */
    public void roll(int floor, Stat stat, List<Item> itemList, GameEvent currentEvent,
                     int rewardMultiplier, int expMultiplier, AiCallback callback) {
        // 상점은 어떤 선택지를 눌러도 상점이 열리므로 선택지를 추가해도 의미가 없다
        if (currentEvent instanceof ShopEvent) {
            postError(callback, "상점 이벤트에는 주사위를 사용할 수 없습니다.");
            return;
        }
        if (currentEvent == null || !currentEvent.canAppendChoice()) {
            postError(callback, "선택지와 보상 개수가 맞지 않는 이벤트입니다.");
            return;
        }

        List<Item> candidates = pickItems(itemList, new Random());
        Set<String> validItemIds = new HashSet<>();
        for (Item item : candidates) validItemIds.add(item.getId());

        String prompt = buildPrompt(floor, stat, candidates, currentEvent);

        // 응답과 전체 시간 초과 중 먼저 온 쪽만 처리한다. 선택지가 두 번 붙는 것을 여기서 최종적으로 막는다
        AtomicBoolean finished = new AtomicBoolean(false);
        Runnable timeout = () -> {
            if (!finished.compareAndSet(false, true)) return;
            Log.w(TAG, "주사위 전체 시간 초과 (" + AiTimeouts.DICE_TOTAL_MS + "ms)");
            backend.cancel();
            callback.onError("혼돈의 신이 응답하지 않습니다 (시간 초과)");
        };
        mainHandler.postDelayed(timeout, AiTimeouts.DICE_TOTAL_MS);

        // 검사를 백엔드에 넘겨서, 온디바이스 응답이 통과하지 못하면 클라우드로 다시 요청하게 한다
        ResponseValidator validator = text -> {
            try {
                parseResponse(text, validItemIds, currentEvent);
                return null;
            } catch (ChoiceLineParser.ParseException e) {
                Log.e(TAG, "파싱 실패(" + e.getMessage() + ") 원문:\n" + text);
                return "혼돈의 결말 해석 실패: " + e.getMessage();
            }
        };

        backend.generate(prompt, ResponseFormat.TEXT, validator, new LlmCallback() {
            @Override
            public void onResult(String text) {
                ChoiceDelta parsed;
                try {
                    parsed = parseResponse(text, validItemIds, currentEvent);
                } catch (ChoiceLineParser.ParseException e) {
                    // validator를 통과한 응답이라 여기로 오지 않는다
                    onError("혼돈의 결말 해석 실패: " + e.getMessage());
                    return;
                }
                // 파서 상한(±9999)은 형식 방어용일 뿐이라, 실제 게임 수치는 층별 상한으로 다시 자른다
                ChoiceDelta clamped = parsed.withClampedStats(floor);
                // 모델은 수치 없이 상황만 묘사하고, 실제로 적용되는 양(상한, 난이도·특성 배수 반영)은 코드가 붙인다
                String summary = clamped.toReward().summary(rewardMultiplier, expMultiplier,
                        itemName(candidates, clamped.getItemId()));
                final ChoiceDelta delta = summary.isEmpty() ? clamped
                        : clamped.withDescription(clamped.getRewardDescription() + " " + summary);
                // 이벤트 객체는 UI가 읽고 있으므로 수정은 메인 스레드에서
                mainHandler.post(() -> {
                    if (!finish(finished, timeout)) return;
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
                mainHandler.post(() -> {
                    if (finish(finished, timeout)) callback.onError(errorMessage);
                });
            }
        });
    }

    private static String itemName(List<Item> candidates, String itemId) {
        if (itemId == null) return null;
        for (Item item : candidates) {
            if (itemId.equals(item.getId())) return item.getName();
        }
        return null;
    }

    /** 먼저 끝난 쪽이면 시간 초과 예약을 지우고 true. 메인 스레드에서만 부른다 */
    private boolean finish(AtomicBoolean finished, Runnable timeout) {
        if (!finished.compareAndSet(false, true)) {
            Log.w(TAG, "시간 초과 뒤에 도착한 응답은 버림");
            return false;
        }
        mainHandler.removeCallbacks(timeout);
        return true;
    }

    private void postError(AiCallback callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }

    /**
     * 응답을 읽고, 기존 선택지나 프롬프트 예시를 그대로 베낀 경우는 실패로 본다.
     * 디버그 빌드의 온디바이스 측정 Activity도 같은 기준을 쓰도록 패키지 범위로 열어 둔다.
     */
    static ChoiceDelta parseResponse(String text, Set<String> validItemIds, GameEvent event)
            throws ChoiceLineParser.ParseException {
        Set<String> forbidden = new HashSet<>();
        forbidden.add(EXAMPLE_CHOICE);
        if (event.getChoices() != null) forbidden.addAll(event.getChoices());
        return ChoiceLineParser.parse(text, validItemIds, forbidden);
    }

    /** 후보 중 최대 ITEM_CANDIDATES개를 무작위로 고른다. 목록이 길면 소형 모델이 형식을 놓친다 */
    static List<Item> pickItems(List<Item> itemList, Random random) {
        List<Item> pool = new ArrayList<>();
        for (Item item : itemList) {
            if (item != null && item.getId() != null) pool.add(item);
        }
        Collections.shuffle(pool, random);
        return pool.size() > ITEM_CANDIDATES ? new ArrayList<>(pool.subList(0, ITEM_CANDIDATES)) : pool;
    }

    /**
     * 이벤트 전체 JSON 대신 필요한 정보만 짧게 넣는다 (입력 토큰도 줄어든다).
     * 형식은 "(20자 내외의 행동)" 같은 괄호 안내문 대신 채워진 예시로 보여 준다.
     * 안내문을 쓰면 소형 온디바이스 모델이 그 문구를 그대로 선택지로 베낀다 (0-A 측정에서 20건 중 19건).
     */
    static String buildPrompt(int floor, Stat stat, List<Item> itemList, GameEvent event) {
        String items = itemList.stream()
                .filter(i -> i != null && i.getId() != null)
                .map(i -> i.getId() + ":" + i.getName())
                .collect(Collectors.joining(", "));
        String choices = event.getChoices() == null ? "" : String.join(" / ", event.getChoices());

        return "[혼돈의 신 | " + floor + "층]\n"
                + "플레이어: 힘 " + stat.getStrength()
                + ", 민첩 " + stat.getAgility()
                + ", 지혜 " + stat.getWisdom()
                + ", HP " + stat.getHp() + "/" + stat.getMaxHp() + "\n"
                + "이벤트: " + event.getName() + "\n"
                + "상황: " + event.getDescription() + "\n"
                + "기존 선택지: " + choices + "\n"
                + "아이템 후보: " + (items.isEmpty() ? "없음" : items) + "\n\n"
                + "기존 선택지와 다른 새 행동 하나와 그 결과를 지어라. 결과는 무작위로 아주 좋거나 아주 나쁘게.\n"
                + "아래 예시처럼 네 줄로만 답하라.\n\n"
                + "선택지: " + EXAMPLE_CHOICE + "\n"
                + "보상설명: " + EXAMPLE_DESCRIPTION + "\n"
                + "아이템: 없음\n"
                + "스탯: " + EXAMPLE_STATS + "\n\n"
                + "규칙: 선택지는 20자 안팎의 행동. 보상설명은 결과를 묘사하는 문장이고 숫자는 쓰지 않는다. "
                + "스탯 줄은 그 묘사와 맞게, 플레이어에게 생긴 변화량만 부호를 붙여 0~3개 적는다(현재 값이 아니라 늘거나 준 양). "
                + "쓸 수 있는 것은 힘, 민첩, 지혜, 경험치, HP. 다치면 HP-수치, 회복하면 HP+수치. "
                + "적에게 준 피해는 적지 않는다. 아이템은 후보 id 하나 또는 없음.";
    }
}