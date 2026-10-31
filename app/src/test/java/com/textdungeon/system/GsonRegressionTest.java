package com.textdungeon.system;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.textdungeon.ai.ChoiceDelta;
import com.textdungeon.data.EventGson;
import com.textdungeon.event.BattleEvent;
import com.textdungeon.event.GameEvent;
import com.textdungeon.event.ShopEvent;
import com.textdungeon.model.Job;
import com.textdungeon.model.RewardStat;
import com.textdungeon.player.Player;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


// ══════════════════════════════════════════════
//  gson 버전 회귀 테스트
//  LiteRT-LM을 넣으면 gson이 2.10.1 → 2.14로 오른다.
//  이벤트 데이터 로드, 세이브 저장/불러오기, 예전 세이브 복구, AI 이벤트 파싱이 그대로인지 확인한다.
// ══════════════════════════════════════════════
public class GsonRegressionTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Context context;
    private Gson eventGson;
    private Map<String, GameEvent> events;

    @Before
    public void setUp() throws Exception {
        context = mock(Context.class);
        when(context.getFilesDir()).thenReturn(tmp.getRoot());
        eventGson = EventGson.create();
        events = loadEventList();
    }

    /** DataControl.init/spawn과 같은 방식: 배열을 객체 단위로 나눈 뒤 EventGson으로 변환 */
    private Map<String, GameEvent> loadEventList() throws Exception {
        String json = new String(Files.readAllBytes(
                new File("src/main/assets/event_list.json").toPath()), StandardCharsets.UTF_8);
        JsonArray array = JsonParser.parseString(json).getAsJsonArray();
        Map<String, GameEvent> result = new TreeMap<>();
        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();
            result.put(obj.get("id").getAsString(), eventGson.fromJson(obj.toString(), GameEvent.class));
        }
        return result;
    }

    private <T extends GameEvent> T firstOf(Class<T> clazz) {
        for (GameEvent e : events.values()) {
            if (e.getClass() == clazz) return clazz.cast(e);
        }
        fail(clazz.getSimpleName() + " 이벤트가 데이터에 없습니다");
        return null;
    }

    private GameSave makeSave() {
        Player player = new Player("테스터", Job.WARRIOR);
        player.getStat().addGold(321);

        Map<Integer, GameEvent> aiEvents = new HashMap<>();
        aiEvents.put(12, firstOf(BattleEvent.class));
        aiEvents.put(15, firstOf(ShopEvent.class));

        // 혼돈의 주사위로 선택지가 붙은 이벤트가 현재 이벤트로 저장되는 경우
        GameEvent current = firstOf(GameEvent.class);
        List<RewardStat> stats = new ArrayList<>();
        stats.add(new RewardStat("힘", 6));
        stats.add(new RewardStat("경험치", 90));
        current.addChoiceWithReward("제단에 손을 얹는다",
                new ChoiceDelta("제단에 손을 얹는다", "제단이 빛을 뿜는다.", null, stats).toReward());

        return new GameSave(player, 12, "dungeon_2", aiEvents, current);
    }

    @Test
    public void 이벤트_데이터가_종류별로_읽힌다() {
        int battle = 0, shop = 0, normal = 0;
        for (GameEvent e : events.values()) {
            assertNotNull(e.getId());
            assertNotNull(e.getId() + " 선택지", e.getChoices());
            if (e instanceof BattleEvent) {
                battle++;
                assertNotNull(e.getId() + " enemyId", ((BattleEvent) e).getEnemyId());
            } else if (e instanceof ShopEvent) {
                shop++;
                assertNotNull(e.getId() + " shopItems", ((ShopEvent) e).getShopItemIds());
            } else {
                normal++;
            }
        }
        assertTrue("전투 이벤트 " + battle, battle > 0);
        assertTrue("상점 이벤트 " + shop, shop > 0);
        assertTrue("일반 이벤트 " + normal, normal > 0);
    }

    @Test
    public void 이벤트를_다시_쓰고_읽어도_같다() {
        for (GameEvent e : events.values()) {
            String once = eventGson.toJson(e, GameEvent.class);
            String twice = eventGson.toJson(eventGson.fromJson(once, GameEvent.class), GameEvent.class);
            assertEquals(e.getId(), once, twice);
        }
    }

    @Test
    public void 세이브를_저장하고_불러오면_그대로다() {
        GameSave save = makeSave();
        assertTrue(save.runSave(context));

        GameSave loaded = GameSave.runLoad(context);
        assertNotNull(loaded);
        assertEquals(12, loaded.getCurrentFloor());
        assertEquals("dungeon_2", loaded.getDungeonId());
        assertEquals(save.getPlayer().getStat().getGold(), loaded.getPlayer().getStat().getGold());

        assertTrue(loaded.getAiEvents().get(12) instanceof BattleEvent);
        assertEquals(((BattleEvent) save.getAiEvents().get(12)).getEnemyId(),
                ((BattleEvent) loaded.getAiEvents().get(12)).getEnemyId());
        assertTrue(loaded.getAiEvents().get(15) instanceof ShopEvent);
        assertEquals(((ShopEvent) save.getAiEvents().get(15)).getShopItemIds(),
                ((ShopEvent) loaded.getAiEvents().get(15)).getShopItemIds());

        GameEvent current = loaded.getCurrentEvent();
        assertEquals(GameEvent.class, current.getClass());
        assertTrue(current.canAppendChoice());
        assertEquals("제단에 손을 얹는다", current.getChoices().get(current.getChoices().size() - 1));

        // 다시 저장했을 때 내용이 한 글자도 달라지지 않아야 한다
        assertEquals(eventGson.toJson(save), eventGson.toJson(loaded));
    }

    @Test
    public void type_없는_예전_세이브도_이벤트_종류가_복구된다() throws Exception {
        GameSave save = makeSave();
        JsonObject root = JsonParser.parseString(eventGson.toJson(save)).getAsJsonObject();
        for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("aiEvents").entrySet()) {
            e.getValue().getAsJsonObject().remove("type");
        }
        root.getAsJsonObject("currentEvent").remove("type");
        Files.write(new File(tmp.getRoot(), "run_save.json").toPath(),
                root.toString().getBytes(StandardCharsets.UTF_8));

        GameSave loaded = GameSave.runLoad(context);
        assertNotNull(loaded);
        assertTrue(loaded.getAiEvents().get(12) instanceof BattleEvent);
        assertNotNull(((BattleEvent) loaded.getAiEvents().get(12)).getEnemyId());
        assertTrue(loaded.getAiEvents().get(15) instanceof ShopEvent);
        assertNotNull(((ShopEvent) loaded.getAiEvents().get(15)).getShopItemIds());
        assertEquals(GameEvent.class, loaded.getCurrentEvent().getClass());
    }

    @Test
    public void 난이도로_저장한_예전_세이브는_던전_id_없이_읽힌다() throws Exception {
        // 던전 전에는 "difficulty":"HARD"로 저장했다. 지금은 모르는 칸이라 버리고, 던전은 DataControlTower가 정한다
        JsonObject root = JsonParser.parseString(eventGson.toJson(makeSave())).getAsJsonObject();
        root.remove("dungeonId");
        root.addProperty("difficulty", "HARD");
        Files.write(new File(tmp.getRoot(), "run_save.json").toPath(),
                root.toString().getBytes(StandardCharsets.UTF_8));

        GameSave loaded = GameSave.runLoad(context);
        assertNotNull(loaded);
        assertNull(loaded.getDungeonId());
        assertEquals(12, loaded.getCurrentFloor());
    }

    @Test
    public void AI_이벤트_JSON이_종류별로_읽힌다() {
        // EventGenerator가 type을 채운 뒤 eventGson.fromJson(JsonObject, GameEvent.class)로 읽는 경로
        String reward = "{\"itemId\":null,\"description\":\"결과\",\"statRewards\":[{\"type\":\"힘\",\"value\":3}]}";
        String battle = "{\"id\":\"ai_1\",\"name\":\"그림자\",\"description\":\"d\",\"minFloor\":5,\"maxFloor\":5,"
                + "\"type\":\"battle\",\"enemyId\":\"monster_1\",\"choices\":[\"싸운다\"],\"rewards\":[" + reward + "]}";
        String shop = "{\"id\":\"ai_2\",\"name\":\"떠돌이 상인\",\"description\":\"d\",\"minFloor\":5,\"maxFloor\":5,"
                + "\"type\":\"shop\",\"shopItems\":[\"item_1\",\"item_2\"],\"choices\":[\"본다\"],\"rewards\":[" + reward + "]}";
        String normal = "{\"id\":\"ai_3\",\"name\":\"제단\",\"description\":\"d\",\"minFloor\":5,\"maxFloor\":5,"
                + "\"type\":\"normal\",\"choices\":[\"기도한다\"],\"rewards\":[" + reward + "]}";

        GameEvent b = eventGson.fromJson(JsonParser.parseString(battle).getAsJsonObject(), GameEvent.class);
        GameEvent s = eventGson.fromJson(JsonParser.parseString(shop).getAsJsonObject(), GameEvent.class);
        GameEvent n = eventGson.fromJson(JsonParser.parseString(normal).getAsJsonObject(), GameEvent.class);

        assertEquals("monster_1", ((BattleEvent) b).getEnemyId());
        assertEquals(java.util.Arrays.asList("item_1", "item_2"), ((ShopEvent) s).getShopItemIds());
        assertEquals(GameEvent.class, n.getClass());
        assertEquals(Collections.singletonList("기도한다"), n.getChoices());
        assertEquals(1, n.getRewards().size());
    }
}
