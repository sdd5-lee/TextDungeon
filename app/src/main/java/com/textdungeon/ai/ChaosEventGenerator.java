package com.textdungeon.ai;

import com.example.textdungeon.BuildConfig;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.textdungeon.event.GameEvent;
import com.textdungeon.model.Item;
import com.textdungeon.model.Stat;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ChaosEventGenerator {
    private final GenerativeModelFutures model;
    private final Gson gson;
    private final Executor executor;

    public ChaosEventGenerator() {
        GenerationConfig.Builder config = new GenerationConfig.Builder();
        config.responseMimeType = "application/json";

        GenerativeModel gm = new GenerativeModel(
                "gemini-1.5-flash",
                BuildConfig.GEMINI_API_KEY,
                config.build()
        );

        this.model = GenerativeModelFutures.from(gm);
        this.gson = new Gson();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void generate(int floor, Stat stat, List<Item> itemList, String eventType, AiCallback callback) {
        String itemNames = itemList.stream()
                .map(Item::getName)
                .collect(Collectors.joining(", "));

        String prompt = String.format(
                "너는 다크 판타지 RPG의 '혼돈의 신'이다. %dF에서 발생할 %s 타입의 새로운 이벤트를 창조하라.\n\n" +
                        "[상황]\n" +
                        "- 현재 층: %dF\n" +
                        "- 플레이어 상태: %s\n" +
                        "- 상점/보상용 아이템 목록: %s\n\n" +
                        "[생성 규칙]\n" +
                        "1. 반드시 JSON 객체 하나만 출력하라. 필드명은 아래와 같아야 한다.\n" +
                        "   - id: 'event_chaos_' + 랜덤숫자\n" +
                        "   - name, description, imgId (imgId는 기존 데이터의 이미지ID 중 선택)\n" +
                        "   - minFloor, maxFloor: %d\n" +
                        "   - type: '%s'\n" +
                        "   - enemyId: (type이 'battle'이면 몬스터ID, 아니면 null)\n" +
                        "   - choices: [선택지1, 선택지2] (무조건 2개)\n" +
                        "   - rewards: [Reward객체1, Reward객체2] (choices 순서와 대응)\n" +
                        "2. statRewards 타입 키워드: \"힘\", \"민첩\", \"체력\", \"지혜\", \"경험치\", \"데미지\", \"회복\", \"골드\"\n" +
                        "3. shopItems: (type이 'shop'이면 위의 아이템 목록에서 3개 선택하여 리스트 작성, 아니면 null 또는 생략)\n" +
                        "4. 모든 필드를 원본 이벤트 구조와 완벽하게 일치시켜라.\n" +
                        "5. 부연 설명 없이 오직 수정된 JSON 객체 하나만 출력하라.\n" +
                        "6. 두가지의 선택지는 반드시 높은 보상과 강한 패널티가 동반되어야한다.",
                floor, eventType, floor, gson.toJson(stat), itemNames, floor, eventType
        );

        Content content = new Content.Builder().addText(prompt).build();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                try {
                    String cleanJson = result.getText().replaceAll("(?s)```json\\s*|\\s*```", "").trim();
                    GameEvent newEvent = gson.fromJson(cleanJson, GameEvent.class);
                    callback.onSuccess(newEvent);
                } catch (Exception e) {
                    callback.onError("이벤트 창조 실패: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onError("네트워크 오류: " + t.getMessage());
            }
        }, executor);
    }
}