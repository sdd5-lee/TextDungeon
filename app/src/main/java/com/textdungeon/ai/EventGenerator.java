package com.textdungeon.ai;

import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.textdungeon.data.RuntimeTypeAdapterFactory;
import com.textdungeon.event.BattleEvent;
import com.textdungeon.event.GameEvent;
import com.textdungeon.event.ShopEvent;
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
                ApiKeyManager.getGeminiGodsKey(),
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
                        "4. 체력감소=데미지(양수), 회복=회복(양수), 영구감소 등 특수효과 없음\n" +
                        "5. 전투에서는 현재 floor ~ +10층에 나오는 몬스터만 소환할 것\n" +
                        "6. type은 항상 battle, normal, shop 중 하나로 지정할 것",
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

                try {
                    String cleanJson = rawText.replaceAll("(?s)```json\\s*|\\s*```", "").trim();

                    RuntimeTypeAdapterFactory<GameEvent> eventAdapterFactory =
                            RuntimeTypeAdapterFactory.of(GameEvent.class, "type")
                                    .registerSubtype(BattleEvent.class, "battle")
                                    .registerSubtype(GameEvent.class, "normal")
                                    .registerSubtype(ShopEvent.class, "shop");

                    Gson eventGson = new GsonBuilder()
                            .registerTypeAdapterFactory(eventAdapterFactory)
                            .create();

                    JsonElement jsonElement = JsonParser.parseString(cleanJson);
                    JsonObject jsonObject;

                    if (jsonElement.isJsonArray() && jsonElement.getAsJsonArray().size() > 0) {
                        jsonObject = jsonElement.getAsJsonArray().get(0).getAsJsonObject();
                    } else {
                        jsonObject = jsonElement.getAsJsonObject();
                    }

                    if (jsonObject.has("shopItems") && !jsonObject.get("shopItems").isJsonNull()) {
                        jsonObject.addProperty("type", "shop");
                    } else if (jsonObject.has("enemyId") && !jsonObject.get("enemyId").isJsonNull()) {
                        jsonObject.addProperty("type", "battle");
                    } else if (!jsonObject.has("type") || jsonObject.get("type").isJsonNull()) {
                        jsonObject.addProperty("type", "normal");
                    }

                    GameEvent newEvent = eventGson.fromJson(jsonObject, GameEvent.class);

                    Log.d(TAG, "JSON 파싱 성공! 생성된 이벤트 이름: " + newEvent.getName());
                    callback.onSuccess(newEvent);

                } catch (Exception e) {
                    Log.e(TAG, "JSON 파싱 실패! AI 응답 데이터: \n" + rawText, e);
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