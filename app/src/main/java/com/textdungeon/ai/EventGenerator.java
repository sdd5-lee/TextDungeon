package com.textdungeon.ai;

import android.util.Log;

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
import com.textdungeon.model.Monster;
import com.textdungeon.model.Stat;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class EventGenerator {
    private static final String TAG = "EventGenerator";
    private final GenerativeModelFutures model;
    private final Gson gson;
    private final Executor executor;

    public EventGenerator() {
        GenerationConfig.Builder config = new GenerationConfig.Builder();
        config.responseMimeType = "application/json";

        GenerativeModel gm = new GenerativeModel(
                "gemini-3-flash-preview",
                BuildConfig.GEMINI_API_KEY_GODS,
                config.build()
        );

        this.model = GenerativeModelFutures.from(gm);
        this.gson = new Gson();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void generate(int floor, Stat stat, List<Item> itemList, List<Monster> monsterList, String eventType, AiType aiType, AiCallback callback) {
        String itemNames = itemList.stream()
                .map(item -> item.getId() + ":" + item.getName())
                .collect(Collectors.joining(", "));

        String monsterNames = monsterList.stream()
                .map(monster -> monster.getId() + ":" + monster.getName())
                .collect(Collectors.joining(", "));
        String prompt = String.format(
                "[%s|%dF|%s] 플레이어:%s 아이템(id:name):%s 몬스터(id:name):%s 성향:%s\n\n" +
                        "아래 JSON 하나만 출력(설명 금지):\n" +
                        "{\"id\":\"%s1\",\"name\":\"\",\"description\":\"\",\"imgId\":\"\",\"minFloor\":%d,\"maxFloor\":%d," +
                        "\"type\":\"%s\",\"enemyId\":null,\"choices\":[\"\",\"\"]," +
                        "\"rewards\":[" +
                        "{\"itemId\":null,\"description\":\"보상묘사\",\"statRewards\":[{\"type\":\"힘\",\"value\":0}]}," +
                        "{\"itemId\":null,\"description\":\"보상묘사\",\"statRewards\":[{\"type\":\"경험치\",\"value\":0}]}" +
                        "],\"shopItems\":null}\n\n" +
                        "[규칙]\n" +
                        "1. rewards.description은 TRPG 서사체로 이득/손해 묘사 (예:'힘 12, 민첩 8 증가')\n" +
                        "2. shopItems: shop타입→아이템목록 3개 / enemyId: battle타입→몬스터목록 id만\n" +
                        "3. statRewards 키워드: 힘|민첩|체력|지혜|경험치|데미지|회복|골드 만 사용\n" +
                        "4. 체력감소=데미지(양수), 회복=회복(양수), 영구감소 등 특수효과 없음",
                aiType.getGodName(), floor, eventType,
                gson.toJson(stat), itemNames, monsterNames, aiType.getRule(),
                aiType.getIdPrefix(), floor, floor, eventType
        );

        Content content = new Content.Builder().addText(prompt).build();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String rawText = result.getText();
                Log.d(TAG, "AI 원본 응답 도착:\n" + rawText);

                try {
                    String cleanJson = rawText.replaceAll("(?s)```json\\s*|\\s*```", "").trim();
                    GameEvent newEvent = gson.fromJson(cleanJson, GameEvent.class);

                    Log.d(TAG, "JSON 파싱 성공! 생성된 이벤트 이름: " + newEvent.getName());
                    callback.onSuccess(newEvent);

                } catch (Exception e) {
                    Log.e(TAG, "JSON 파싱 실패!", e);
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