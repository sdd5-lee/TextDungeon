package com.textdungeon.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.textdungeon.data.EventGson;
import com.textdungeon.event.BattleEvent;
import com.textdungeon.event.GameEvent;
import com.textdungeon.event.ShopEvent;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

// ══════════════════════════════════════════════
//  던전과 콘텐츠 검사 (assets의 dungeon·event·monster·item 목록을 직접 읽는다)
//  - 던전은 앞 던전을 클리어해야 열리는 한 줄이고, 뒤 던전일수록 깊다
//  - 마지막 층에는 그 던전의 보스가 있고, 보스에게서는 도망칠 수 없다
//  - 던전마다 1층부터 마지막 층까지 모든 층에 이벤트가 있다
//  - 일반 몬스터는 기술 1개, 한 층에만 나오는 보스는 기술 2개
//  - 모든 몬스터·아이템이 어느 이벤트에선가 나온다 (안 나오면 게임에서 볼 수 없다)
// ══════════════════════════════════════════════
public class DungeonContentTest {

    private static final File ASSETS = new File("src/main/assets");
    private static List<Dungeon> dungeons;
    private static Map<String, GameEvent> events;
    private static Map<String, Monster> monsters;
    private static Map<String, Item> items;

    @BeforeClass
    public static void load() throws Exception {
        Gson gson = new Gson();
        Gson eventGson = EventGson.create();
        dungeons = new ArrayList<>(read("dungeon_list.json", e -> gson.fromJson(e, Dungeon.class), Dungeon::getId).values());
        events = read("event_list.json", e -> eventGson.fromJson(e, GameEvent.class), GameEvent::getId);
        monsters = read("monster_list.json", e -> gson.fromJson(e, Monster.class), Monster::getId);
        items = read("item_list.json", e -> gson.fromJson(e, Item.class), Item::getId);
    }

    /** json 배열을 순서대로 읽는다. id가 겹치면 실패 */
    private static <T> Map<String, T> read(String file, Function<JsonElement, T> parse, Function<T, String> id) throws Exception {
        Map<String, T> map = new LinkedHashMap<>();
        try (Reader r = Files.newBufferedReader(new File(ASSETS, file).toPath(), StandardCharsets.UTF_8)) {
            for (JsonElement e : JsonParser.parseReader(r).getAsJsonArray()) {
                T t = parse.apply(e);
                assertNull(file + " id 겹침: " + id.apply(t), map.put(id.apply(t), t));
            }
        }
        return map;
    }

    private static List<GameEvent> eventsOn(int floor, Dungeon d) {
        List<GameEvent> result = new ArrayList<>();
        for (GameEvent e : events.values()) {
            if (floor >= e.getMinFloor() && floor <= e.getMaxFloor() && e.appearsIn(d.getId())) result.add(e);
        }
        return result;
    }

    // ── 던전 ─────────────────────────────────────────

    @Test
    public void 던전은_앞_던전을_클리어해야_열리고_뒤로_갈수록_깊다() {
        assertEquals(4, dungeons.size());
        assertNull("첫 던전은 처음부터 열림", dungeons.get(0).getRequires());
        for (int i = 1; i < dungeons.size(); i++) {
            Dungeon prev = dungeons.get(i - 1), d = dungeons.get(i);
            assertEquals(d.getId(), prev.getId(), d.getRequires());
            assertTrue(d.getId() + " 클리어 층", d.getClearFloor() > prev.getClearFloor());
            assertTrue(d.getId() + " 클리어 보상", d.getClearGems() >= prev.getClearGems());
        }
        assertEquals(75, dungeons.get(dungeons.size() - 1).getClearFloor());
    }

    @Test
    public void 앞_던전을_깨면_다음_던전만_열린다() {
        Set<String> cleared = new HashSet<>();
        assertTrue(dungeons.get(0).isOpen(cleared));
        assertFalse(dungeons.get(1).isOpen(cleared));
        cleared.add(dungeons.get(0).getId());
        assertTrue(dungeons.get(1).isOpen(cleared));
        assertFalse(dungeons.get(2).isOpen(cleared));
        assertFalse("예전 기록(null)은 첫 던전만", dungeons.get(1).isOpen(null));
    }

    @Test
    public void 던전마다_이름_설명_그림_배수가_있다() {
        for (Dungeon d : dungeons) {
            assertFalse(d.getId(), d.getName() == null || d.getName().isEmpty());
            assertFalse(d.getId(), d.getDescription() == null || d.getDescription().isEmpty());
            assertFalse(d.getId(), d.getClearText() == null || d.getClearText().isEmpty());
            assertNotNull(d.getId(), d.getImgId());
            assertTrue(d.getId(), d.getStatMultiplier() >= 1 && d.getRewardMultiplier() >= 1);
            assertTrue(d.getId(), d.getClearGems() > 0);
        }
    }

    @Test
    public void AI_이벤트는_마지막_층보다_앞에서_나온다() {
        for (Dungeon d : dungeons) {
            assertTrue(d.getId(), d.getAiEvents() >= 1);
            assertTrue(d.getId(), Dungeon.aiEventFloor(d.getAiEvents() - 1) < d.getClearFloor());
        }
        assertEquals(1, Dungeon.aiEventFloor(0));
        assertEquals(61, Dungeon.aiEventFloor(3));
    }

    @Test
    public void 마지막_층에는_도망칠_수_없는_보스가_있다() {
        for (Dungeon d : dungeons) {
            GameEvent boss = events.get(d.getBossEventId());
            assertNotNull(d.getId() + " 보스 이벤트 " + d.getBossEventId(), boss);
            assertTrue(d.getId(), boss instanceof BattleEvent);
            assertEquals(d.getId(), d.getClearFloor(), boss.getMinFloor());
            assertEquals(d.getId(), d.getClearFloor(), boss.getMaxFloor());
            assertTrue(d.getId(), boss.appearsIn(d.getId()));
            for (int i = 0; i < boss.getRewards().size(); i++) {
                if (!"battle".equals(boss.getRewards().get(i).getAction())) {
                    assertTrue(d.getId() + " 보스의 도망 선택지는 retry", boss.isRetry(i));
                }
            }
            assertEquals(d.getId() + " 보스 몬스터는 기술 2개", 2,
                    monsters.get(((BattleEvent) boss).getEnemyId()).getPatterns().size());
        }
    }

    @Test
    public void 던전마다_모든_층에_이벤트가_있다() {
        for (Dungeon d : dungeons) {
            for (int floor = 1; floor <= d.getClearFloor(); floor++) {
                assertFalse(d.getId() + " " + floor + "층", eventsOn(floor, d).isEmpty());
            }
        }
    }

    @Test
    public void 보스_층이_아닌_층에는_전투가_있다() {
        Dungeon deepest = dungeons.get(dungeons.size() - 1);
        for (int floor = 1; floor < deepest.getClearFloor(); floor++) {
            boolean battle = false;
            for (GameEvent e : eventsOn(floor, deepest)) battle |= e instanceof BattleEvent;
            // 7·15·22층처럼 한 층짜리 보물 이벤트만 있는 층은 없다 (보물 층도 그 층대 이벤트가 함께 나온다)
            assertTrue(floor + "층에 전투가 없음", battle);
        }
    }

    // ── 몬스터·아이템 ─────────────────────────────────

    @Test
    public void 콘텐츠는_예전의_두_배() {
        assertTrue("몬스터 " + monsters.size(), monsters.size() >= 72);
        assertTrue("아이템 " + items.size(), items.size() >= 96);
        assertTrue("이벤트 " + events.size(), events.size() >= 122);
    }

    @Test
    public void 일반_몬스터는_기술_1개_보스는_2개() {
        for (Monster m : monsters.values()) {
            boolean boss = false, appears = false;
            for (GameEvent e : events.values()) {
                if (e instanceof BattleEvent && m.getId().equals(((BattleEvent) e).getEnemyId())) {
                    appears = true;
                    boss |= e.getMinFloor() == e.getMaxFloor();
                }
            }
            assertTrue(m.getId() + " 나오는 이벤트 없음", appears);
            assertEquals(m.getId() + " " + m.getName(), boss ? 2 : 1, m.getPatterns().size());
            assertEquals(m.getId(), boss, m.isBoss());
            for (MonsterPattern p : m.getPatterns()) {
                assertFalse(m.getId(), p.getName() == null || p.getName().isEmpty());
                assertTrue(m.getId() + " " + p.getName(), p.getPower() > 0 && p.getHits() >= 1);
            }
            assertTrue(m.getId() + " 나오는 층수", m.getDescription().contains("[나오는 층수]"));
        }
    }

    @Test
    public void 이벤트가_가리키는_몬스터와_아이템이_있다() {
        for (GameEvent e : events.values()) {
            if (e instanceof BattleEvent) {
                assertNotNull(e.getId(), monsters.get(((BattleEvent) e).getEnemyId()));
            }
            if (e instanceof ShopEvent) {
                for (String id : ((ShopEvent) e).getShopItemIds()) assertNotNull(e.getId() + " " + id, items.get(id));
            }
            assertEquals(e.getId(), e.getChoices().size(), e.getRewards().size());
            for (Reward r : e.getRewards()) {
                if (r.getItemId() != null) assertNotNull(e.getId() + " " + r.getItemId(), items.get(r.getItemId()));
            }
        }
    }

    @Test
    public void 새_아이템은_모두_어느_이벤트에선가_나온다() {
        Set<String> used = new HashSet<>();
        for (GameEvent e : events.values()) {
            for (Reward r : e.getRewards()) if (r.getItemId() != null) used.add(r.getItemId());
            if (e instanceof ShopEvent) used.addAll(((ShopEvent) e).getShopItemIds());
        }
        List<String> unused = new ArrayList<>();
        for (Item i : items.values()) {
            // item_25 천갑옷은 확장 전부터 어느 이벤트에도 없다 (보물 특성의 무작위 시작 아이템으로만 나온다)
            if (!used.contains(i.getId()) && !"item_25".equals(i.getId())) unused.add(i.getId());
        }
        assertEquals("이벤트에 안 나오는 아이템", Collections.emptyList(), unused);
    }

    @Test
    public void 모든_아이템에_등급이_바르게_적혀_있다() throws Exception {
        // Gson은 모르는 등급을 null(→ 일반)로 읽어 오타가 숨으므로 글자 그대로 본다
        Set<String> names = new HashSet<>();
        for (ItemGrade g : ItemGrade.values()) names.add(g.name());
        try (Reader r = Files.newBufferedReader(new File(ASSETS, "item_list.json").toPath(), StandardCharsets.UTF_8)) {
            for (JsonElement e : JsonParser.parseReader(r).getAsJsonArray()) {
                String id = e.getAsJsonObject().get("id").getAsString();
                assertTrue(id + " grade 없음", e.getAsJsonObject().has("grade"));
                assertTrue(id + " grade 값", names.contains(e.getAsJsonObject().get("grade").getAsString()));
            }
        }
    }

    @Test
    public void 소모품은_회복량이_클수록_등급이_같거나_높다() {
        List<Item> potions = new ArrayList<>();
        for (Item i : items.values()) if ("consumables".equals(i.getType())) potions.add(i);
        potions.sort((a, b) -> Integer.compare(a.getHp(), b.getHp()));
        for (int i = 1; i < potions.size(); i++) {
            Item low = potions.get(i - 1), high = potions.get(i);
            assertTrue(low.getName() + " → " + high.getName(), high.getGrade().ordinal() >= low.getGrade().ordinal());
        }
        assertEquals(ItemGrade.COMMON, potions.get(0).getGrade());
        assertEquals(ItemGrade.LEGENDARY, potions.get(potions.size() - 1).getGrade());
    }

    @Test
    public void 등급마다_아이템이_있고_전설은_드물다() {
        int[] count = new int[ItemGrade.values().length];
        for (Item i : items.values()) count[i.getGrade().ordinal()]++;
        for (ItemGrade g : ItemGrade.values()) assertTrue(g.displayName, count[g.ordinal()] > 0);
        assertTrue("전설 " + count[ItemGrade.LEGENDARY.ordinal()], count[ItemGrade.LEGENDARY.ordinal()] * 5 <= items.size());
        assertEquals(ItemGrade.COMMON, items.get("item_1").getGrade());
        assertEquals(ItemGrade.COMMON, items.get("item_25").getGrade());
    }

    @Test
    public void 상점에서_파는_아이템은_값이_있다() {
        for (GameEvent e : events.values()) {
            if (!(e instanceof ShopEvent)) continue;
            for (String id : ((ShopEvent) e).getShopItemIds()) {
                assertTrue(e.getId() + " " + id + " 가격", items.get(id).getValue() > 0);
            }
        }
    }
}
