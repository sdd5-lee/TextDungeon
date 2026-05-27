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
                BuildConfig.GEMINI_API_KEY,
                config.build()
        );

        this.model = GenerativeModelFutures.from(gm);
        this.gson = new Gson();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void roll(int floor, Stat stat, List<Item> itemList, GameEvent currentEvent, AiCallback callback) {
        String itemNames = itemList.stream()
                .map(Item::getName)
                .filter(name -> name != null && !name.isEmpty())
                .collect(Collectors.joining(", "));

        String prompt = String.format(
                "너는 다크 판타지 RPG의 '혼돈의 신'이다. 플레이어의 요청에 따라 기존 이벤트를 기괴하게 뒤틀어라.\n\n" +
                        "[상황]\n" +
                        "- 층수: %dF\n" +
                        "- 플레이어 상태: %s\n" +
                        "- 사용 가능한 아이템 목록: %s\n" +
                        "- 원본 이벤트: %s\n\n" +
                        "[혼돈의 권능 행사 규칙]\n" +
                        "1. 원본 데이터(id, name, description, choices, rewards)의 기존 내용은 절대 수정하지 마라.\n" +
                        "2. 'choices' 배열의 마지막에 3번째 선택지를 추가하라. (20자 내외로 짧고 강렬하게)\n" +
                        "3. 'rewards' 배열의 마지막에 그 선택지에 대응하는 Reward 객체를 추가하라.\n" +
                        "   - itemId: 아이템 보상을 줄 경우 [사용 가능한 아이템 목록]에 있는 이름 중 하나를 선택하되 이벤트와 맞지 않으면 null을 넣는다.\n" +
                        "   - statRewards: [{\"type\": \"타입\", \"value\": 수치}] 형식으로 작성하라.\n" +
                        "4. statRewards의 'type'은 반드시 자바 gainStat 메서드와 일치하는 키워드만 사용: \"힘\", \"민첩\", \"체력\", \"지혜\", \"경험치\", \"데미지\", \"회복\"\n" +
                        "   - 중요: 체력을 깎으려면 type을 \"데미지\", value는 양수 / 채우려면 type을 \"회복\", value는 양수\n" +
                        "5. 부연 설명 없이 오직 수정된 JSON 객체 하나만 출력하라.\n" +
                        "6. 반드시 원본 데이터의 모든 필드를 포함하고, 완전한 전체 JSON 객체를 출력하라. 생략하지 마라.\n" +
                        "7. 반드시 데이터 description에 어떤 보상을 받았는지 출력하라 생략하지마라.",
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