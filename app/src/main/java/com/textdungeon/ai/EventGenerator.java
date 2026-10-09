package com.textdungeon.ai;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.textdungeon.ai.backend.LlmBackend;
import com.textdungeon.ai.backend.LlmCallback;
import com.textdungeon.ai.backend.ResponseFormat;
import com.textdungeon.data.EventGson;
import com.textdungeon.event.GameEvent;
import com.textdungeon.model.Item;
import com.textdungeon.model.Monster;
import com.textdungeon.model.Stat;

import com.google.gson.JsonArray;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 이벤트 생성 (클라우드 유지 대상).
 * 프롬프트와 JSON 파싱은 기존과 동일하고, HTTP 호출만 LlmBackend로 분리했다.
 * 나중에 이벤트도 온디바이스로 옮길 때는 이 클래스에 넘기는 backend만 바꾸면 된다.
 */
public class EventGenerator {
    private static final String TAG = "EventGenerator";

    private final LlmBackend backend;
    private final Gson gson = new Gson();
    private final Gson eventGson;

    public EventGenerator(LlmBackend backend) {
        this.backend = backend;
        this.eventGson = EventGson.create();
    }

    public void generate(int floor, Stat stat, List<Item> itemList, List<Monster> monsterList,
                         String eventType, AiType aiType, AiCallback callback) {
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
                        "{\"action\":null,\"itemId\":null,\"description\":\"보상묘사\",\"statRewards\":[{\"type\":\"힘\",\"value\":0}]}," +
                        "{\"action\":null,\"itemId\":null,\"description\":\"보상묘사\",\"statRewards\":[{\"type\":\"경험치\",\"value\":0}]}" +
                        "],\"shopItems\":null}\n\n" +
                        "[규칙]\n" +
                        "1. rewards.description은 TRPG 서사체로 이득/손해 묘사 (예:'힘 12, 민첩 8 증가')\n" +
                        "2. shopItems: shop타입→아이템목록 3개 / enemyId: battle타입→몬스터목록 id만\n" +
                        "3. statRewards 키워드: 힘|민첩|체력|지혜|경험치|데미지|회복|골드 만 사용\n" +
                        "4. 체력감소=데미지(양수), 회복=회복(양수), 영구감소 등 특수효과 없음\n" +
                        "5. enemyId는 위 몬스터 목록(이 층에 맞는 몬스터만 들어 있음)의 id만 사용할 것\n" +
                        "6. battle타입: choices[0]은 몬스터와 싸우는 행동, rewards[0].action=\"battle\"이고 보상은 승리했을 때의 보상. " +
                        "choices[1]은 피하는 행동, rewards[1].action=\"escape\". normal/shop타입은 action을 null로 둘 것\n" +
                        "7. type은 항상 battle, normal, shop 중 하나로 지정할 것",
                aiType.getGodName(), floor, eventType,
                gson.toJson(stat), itemNames, monsterNames, aiType.getRule(),
                aiType.getIdPrefix(), floor, floor, eventType
        );

        backend.generate(prompt, ResponseFormat.JSON, new LlmCallback() {
            @Override
            public void onResult(String text) {
                parseAndCallback(text, itemList, monsterList, aiType, callback);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    private void parseAndCallback(String rawText, List<Item> itemList, List<Monster> monsterList,
                                  AiType aiType, AiCallback callback) {
        try {
            String cleanJson = rawText.replaceAll("(?s)```json\\s*|\\s*```", "").trim();

            JsonElement jsonElement = JsonParser.parseString(cleanJson);
            JsonObject jsonObject;

            if (jsonElement.isJsonArray() && jsonElement.getAsJsonArray().size() > 0) {
                jsonObject = jsonElement.getAsJsonArray().get(0).getAsJsonObject();
            } else {
                jsonObject = jsonElement.getAsJsonObject();
            }

            if (aiType == AiType.TREASURE) {
                // 보물의 신은 상점이 아니라 보물을 골라 받는 일반 이벤트 (타입 판별보다 먼저 정리)
                fixTreasureEvent(jsonObject, itemList);
            }

            if (jsonObject.has("shopItems") && !jsonObject.get("shopItems").isJsonNull()) {
                jsonObject.addProperty("type", "shop");
            } else if (jsonObject.has("enemyId") && !jsonObject.get("enemyId").isJsonNull()) {
                jsonObject.addProperty("type", "battle");
            } else if (!jsonObject.has("type") || jsonObject.get("type").isJsonNull()) {
                jsonObject.addProperty("type", "normal");
            }

            if ("battle".equals(jsonObject.get("type").getAsString())) {
                fixBattleEvent(jsonObject, monsterList);
            }
            // 코드가 보정한 뒤 실제로 게임에 들어가는 형태 (AI 원문과 비교용)
            com.textdungeon.ai.backend.GeminiBackend.logRaw("보정 후 이벤트", jsonObject.toString());

            GameEvent newEvent = eventGson.fromJson(jsonObject, GameEvent.class);

            Log.d(TAG, "JSON 파싱 성공! 생성된 이벤트 이름: " + newEvent.getName());
            callback.onSuccess(newEvent);

        } catch (Exception e) {
            Log.e(TAG, "JSON 파싱 실패! rawText: \n" + rawText, e);
            callback.onError("이벤트 창조 실패: " + e.getMessage());
        }
    }

    /**
     * 보물의 신 이벤트 보정: 상점/전투 요소를 지우고, 선택지마다 서로 다른 보물이 하나씩 붙도록 한다.
     * AI가 itemId를 빠뜨리거나, 목록에 없는 id를 주거나, 같은 보물을 두 번 넣으면 남은 보물로 채운다.
     * itemList에는 DifficultyActivity가 넘긴 심층 장비만 들어 있다.
     */
    private void fixTreasureEvent(JsonObject event, List<Item> itemList) {
        event.add("shopItems", com.google.gson.JsonNull.INSTANCE);
        event.add("enemyId", com.google.gson.JsonNull.INSTANCE);
        event.addProperty("type", "normal");

        if (!event.has("rewards") || !event.get("rewards").isJsonArray()) return;
        JsonArray rewards = event.getAsJsonArray("rewards");

        Set<String> valid = new HashSet<>();
        for (Item item : itemList) {
            if (item != null && item.getId() != null) valid.add(item.getId());
        }
        List<String> pool = new java.util.ArrayList<>(valid);
        java.util.Collections.shuffle(pool);

        Set<String> used = new HashSet<>();
        for (JsonElement r : rewards) {
            if (!r.isJsonObject()) continue;
            JsonObject reward = r.getAsJsonObject();
            reward.add("action", com.google.gson.JsonNull.INSTANCE);

            String id = reward.has("itemId") && !reward.get("itemId").isJsonNull()
                    ? reward.get("itemId").getAsString() : null;
            if (id == null || !valid.contains(id) || used.contains(id)) {
                String replacement = null;
                for (String candidate : pool) {
                    if (!used.contains(candidate)) { replacement = candidate; break; }
                }
                Log.w(TAG, "보물 itemId(" + id + ") → " + replacement + "로 교체");
                id = replacement;
                if (id == null) {
                    reward.add("itemId", com.google.gson.JsonNull.INSTANCE);
                    continue;
                }
                reward.addProperty("itemId", id);
            }
            used.add(id);
        }
    }

    /**
     * AI가 만든 전투 이벤트를 실제로 전투가 시작되는 형태로 보정한다.
     *
     * 1) 전투는 enemyId만으로 시작되지 않고, 고른 선택지의 보상에 action="battle"이 있어야 시작된다
     *    (EventActivity.onChoiceSelected). AI가 이걸 빠뜨리면 싸우는 선택지를 눌러도 전투 없이
     *    승리 보상만 들어가므로, battle 표시가 하나도 없으면 첫 선택지=전투, 둘째=도망으로 채운다.
     * 2) 목록에 없는 몬스터 id면 전투창을 열 수 없으므로, 전달받은 (층에 맞는) 몬스터 중 하나로 바꾼다.
     */
    private void fixBattleEvent(JsonObject event, List<Monster> monsterList) {
        // 1) 몬스터 id 검증
        Set<String> validIds = new HashSet<>();
        for (Monster m : monsterList) {
            if (m != null && m.getId() != null) validIds.add(m.getId());
        }
        String enemyId = event.has("enemyId") && !event.get("enemyId").isJsonNull()
                ? event.get("enemyId").getAsString() : null;
        if ((enemyId == null || !validIds.contains(enemyId)) && !monsterList.isEmpty()) {
            String replacement = monsterList.get(new Random().nextInt(monsterList.size())).getId();
            Log.w(TAG, "잘못된 enemyId(" + enemyId + ") → " + replacement + "로 교체");
            event.addProperty("enemyId", replacement);
        }

        // 2) 전투 선택지 표시
        if (!event.has("rewards") || !event.get("rewards").isJsonArray()) return;
        JsonArray rewards = event.getAsJsonArray("rewards");
        if (rewards.size() == 0) return;

        boolean hasBattle = false;
        for (JsonElement r : rewards) {
            if (r.isJsonObject()) {
                JsonElement action = r.getAsJsonObject().get("action");
                if (action != null && !action.isJsonNull() && "battle".equals(action.getAsString())) {
                    hasBattle = true;
                    break;
                }
            }
        }
        if (hasBattle) return;

        Log.w(TAG, "전투 이벤트에 action=battle이 없어 첫 선택지를 전투로 지정");
        if (rewards.get(0).isJsonObject()) {
            rewards.get(0).getAsJsonObject().addProperty("action", "battle");
        }
        if (rewards.size() > 1 && rewards.get(1).isJsonObject()) {
            JsonObject second = rewards.get(1).getAsJsonObject();
            if (!second.has("action") || second.get("action").isJsonNull()) {
                second.addProperty("action", "escape");
            }
        }
    }
}