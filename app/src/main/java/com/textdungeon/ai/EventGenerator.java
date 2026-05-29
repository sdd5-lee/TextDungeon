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

    public void generate(int floor, Stat stat, List<Item> itemList, String eventType, AiType aiType, AiCallback callback) {
        String itemNames = itemList.stream()
                .map(item -> item.getId() + ":" + item.getName())
                .collect(Collectors.joining(", "));

        String prompt = String.format(
                "[%s | %dF | %s]\n" +
                        "플레이어: %s\n" +
                        "아이템(id:이름 형식, 반드시 이 목록의 id만 사용): %s\n" +
                        "성향: %s\n\n" +
                        "아래 구조로 JSON 하나만 출력:\n" +
                        "{\"id\":\"%s1\",\"name\":\"\",\"description\":\"\",\"imgId\":\"\",\"minFloor\":%d,\"maxFloor\":%d,\"type\":\"%s\"," +
                        "\"enemyId\":null,\"choices\":[\"\",\"\"],\"rewards\":[" +
                        "{\"itemId\":null,\"statRewards\":[{\"힘\":0}]}," +
                        "{\"itemId\":null,\"statRewards\":[{\"경험치\":0}]}]," +
                        "\"shopItems\":null}\n" +
                        "shopItems는 type이 shop이면 아이템목록서 3개, enemyId는 type이 battle이면 몬스터ID.\n" +
                        "설명 금지."+"statRewards 형식: [{\"type\":\"키워드\",\"value\":수치}]\n" +
                        "키워드는 반드시 이것만 사용: 힘,민첩,체력,지혜,경험치,데미지,회복,골드\n" +
                        "체력감소=데미지 양수 / 회복=회복 양수 / 영구감소 등 특수효과 없음\n",
                aiType.getGodName(), floor, eventType,
                gson.toJson(stat), itemNames,
                aiType.getRule(),
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