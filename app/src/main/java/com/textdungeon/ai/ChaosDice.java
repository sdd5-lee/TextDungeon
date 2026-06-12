package com.textdungeon.ai;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.textdungeon.event.BattleEvent;
import com.textdungeon.event.GameEvent;
import com.textdungeon.model.Item;
import com.textdungeon.model.Stat;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
public class ChaosDice {
    private final GenerativeModelFutures model;
    private final Gson gson;
    private final Executor executor;

    public ChaosDice() {
        GenerationConfig.Builder config = new GenerationConfig.Builder();
        config.responseMimeType = "application/json";

        GenerativeModel gm = new GenerativeModel(
                "gemini-3-flash-preview",
                ApiKeyManager.getGeminiKey(),
                config.build()
        );

        this.model = GenerativeModelFutures.from(gm);
        this.gson = new Gson();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void roll(int floor, Stat stat, List<Item> itemList, GameEvent currentEvent, AiCallback callback) {
        String itemNames = itemList.stream()
                .map(item -> item.getId() + ":" + item.getName())
                .filter(name -> name != null && !name.isEmpty())
                .collect(Collectors.joining(", "));

        String prompt = String.format(
                "[혼돈의 신 | %dF]\n" +
                        "플레이어: %s\n" +
                        "아이템(id:이름 형식, 반드시 이 목록의 id만 사용): %s\n" +
                        "원본 이벤트: %s\n\n" +
                        "choices 마지막에 선택지 1개 추가(20자 내외), rewards 마지막에 대응 Reward 추가.\n" +
                        "보상은 최하/최상 중 랜덤. itemId는 아이템목록 중 선택(맞지않으면 null).\n" +
                        "statRewards: [{\"type\":\"타입\",\"value\":수치}] / 키워드: 힘,민첩,체력,지혜,경험치,데미지,회복\n" +
                        "체력감소=데미지 양수 / 체력회복=회복 양수\n" +
                        "description에 보상 내용 포함. 전체 JSON만 출력. 생략/설명 금지.",
                floor, gson.toJson(stat), itemNames, gson.toJson(currentEvent)
        );

        Content content = new Content.Builder().addText(prompt).build();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                try {
                    String resultText = result.getText();
                    String cleanJson = resultText.replaceAll("(?s)```json\\s*|\\s*```", "").trim();

                    BattleEvent updatedEvent = gson.fromJson(cleanJson, BattleEvent.class);
                    callback.onSuccess(updatedEvent);
                } catch (Exception e) {
                    callback.onError("혼돈의 결말 해석 실패: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onError("네트워크 오류: " + t.getMessage());
            }
        }, executor);
    }
}