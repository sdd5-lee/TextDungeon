package com.textdungeon.ai;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// ══════════════════════════════════════════════
//  결과표 내용 검사 (assets/chaos_table.json을 직접 읽는다)
//  - 상점이 아닌 이벤트마다 일반 선택지 3개 이상, 특별 선택지 1개 이상
//  - 좋은 결과는 손해가 없고, 나쁜 결과는 손해가 있다
//  - 특별 선택지는 항상 좋은 결과이고 강화된 보상 (깊은 층 장비 또는 상한의 120% 이상)
//  - 묘사에는 숫자를 쓰지 않는다 (실제 적용량은 코드가 붙인다)
// ══════════════════════════════════════════════
public class ChaosTableContentTest {

    private static final File ASSETS = new File("src/main/assets");
    private static final Set<String> STATS = new HashSet<>(Arrays.asList(
            "힘", "민첩", "체력", "지혜", "경험치", "골드", "회복", "데미지"));
    private static final Set<String> ITEMS = new HashSet<>(Arrays.asList(ChaosTable.ITEM_FLOOR, ChaosTable.ITEM_UPPER));
    private static final int MAX_CHOICE_LENGTH = 25;

    private static JsonObject table;
    /** 상점이 아닌 이벤트 ID → 원래 선택지 */
    private static final Map<String, List<String>> events = new HashMap<>();

    @BeforeClass
    public static void load() throws Exception {
        assertTrue("assets 폴더를 못 찾음 (작업 폴더가 app 모듈이어야 함)", ASSETS.isDirectory());
        table = read("chaos_table.json").getAsJsonObject();
        for (JsonElement e : read("event_list.json").getAsJsonArray()) {
            JsonObject o = e.getAsJsonObject();
            if ("shop".equals(o.get("type").getAsString())) continue;
            List<String> choices = new ArrayList<>();
            for (JsonElement c : o.getAsJsonArray("choices")) choices.add(c.getAsString());
            events.put(o.get("id").getAsString(), choices);
        }
    }

    private static JsonElement read(String name) throws Exception {
        try (Reader r = new InputStreamReader(Files.newInputStream(new File(ASSETS, name).toPath()), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(r);
        }
    }

    @Test
    public void 상점이_아닌_이벤트마다_표가_있다() {
        JsonObject byEvent = table.getAsJsonObject("events");
        for (String id : events.keySet()) {
            assertTrue("표 없음: " + id, byEvent.has(id));
        }
        for (String id : byEvent.keySet()) {
            assertTrue("없는 이벤트이거나 상점: " + id, events.containsKey(id));
        }
    }

    @Test
    public void 이벤트별_표와_종류별_표의_구성() {
        for (Map.Entry<String, JsonElement> e : table.getAsJsonObject("events").entrySet()) {
            checkSection("events." + e.getKey(), e.getValue().getAsJsonObject(), 3, events.get(e.getKey()));
        }
        for (String type : new String[]{"battle", "normal"}) {
            JsonObject section = table.getAsJsonObject("types").getAsJsonObject(type);
            assertTrue("종류별 표 없음: " + type, section != null);
            checkSection("types." + type, section, 5, new ArrayList<>());
        }
    }

    private static void checkSection(String where, JsonObject section, int minChoices, List<String> original) {
        JsonArray choices = section.getAsJsonArray("choices");
        JsonArray specials = section.getAsJsonArray("specials");
        assertTrue(where + ": 일반 선택지 " + minChoices + "개 이상", choices != null && choices.size() >= minChoices);
        assertTrue(where + ": 특별 선택지 1개 이상", specials != null && specials.size() >= 1);

        Set<String> seen = new HashSet<>();
        for (String c : original) seen.add(c.replace(" ", ""));
        for (JsonElement el : choices) {
            JsonObject entry = el.getAsJsonObject();
            String choice = checkChoice(where, entry, seen);
            checkOutcome(where + " '" + choice + "' good", entry.getAsJsonObject("good"), Kind.GOOD);
            checkOutcome(where + " '" + choice + "' bad", entry.getAsJsonObject("bad"), Kind.BAD);
        }
        for (JsonElement el : specials) {
            JsonObject special = el.getAsJsonObject();
            String choice = checkChoice(where, special, seen);
            checkOutcome(where + " '" + choice + "' special", special, Kind.SPECIAL);
        }
    }

    /** 선택지 문장: 비어 있지 않고, 짧고, 원래 선택지·같은 표의 다른 선택지와 겹치지 않는다 */
    private static String checkChoice(String where, JsonObject o, Set<String> seen) {
        assertTrue(where + ": choice 없음", o.has("choice"));
        String choice = o.get("choice").getAsString();
        assertTrue(where + ": 빈 선택지", !choice.trim().isEmpty());
        assertTrue(where + ": 선택지가 " + MAX_CHOICE_LENGTH + "자를 넘음: " + choice, choice.length() <= MAX_CHOICE_LENGTH);
        assertTrue(where + ": 선택지가 겹침: " + choice, seen.add(choice.replace(" ", "")));
        return choice;
    }

    private enum Kind { GOOD, BAD, SPECIAL }

    private static void checkOutcome(String where, JsonObject o, Kind kind) {
        if (o == null) fail(where + ": 결과 없음");
        String text = o.has("text") ? o.get("text").getAsString() : "";
        assertTrue(where + ": 묘사 없음", !text.trim().isEmpty());
        assertTrue(where + ": 묘사에 숫자가 있음: " + text, !text.matches(".*\\d.*"));

        String item = o.has("item") ? o.get("item").getAsString() : null;
        assertTrue(where + ": 모르는 item: " + item, item == null || ITEMS.contains(item));

        boolean loss = false;
        int maxPercent = 0;
        JsonObject stats = o.has("stats") ? o.getAsJsonObject("stats") : new JsonObject();
        for (Map.Entry<String, JsonElement> s : stats.entrySet()) {
            assertTrue(where + ": 모르는 스탯: " + s.getKey(), STATS.contains(s.getKey()));
            int v = s.getValue().getAsInt();
            assertTrue(where + ": 0인 스탯: " + s.getKey(), v != 0);
            int limit = kind == Kind.SPECIAL ? ChaosTable.SPECIAL_LIMIT_PERCENT : ChaosTable.NORMAL_LIMIT_PERCENT;
            assertTrue(where + ": " + s.getKey() + " " + v + "%가 " + limit + "%를 넘음", Math.abs(v) <= limit);
            if (v < 0 || "데미지".equals(s.getKey())) loss = true;
            maxPercent = Math.max(maxPercent, v);
        }

        switch (kind) {
            case GOOD:
                assertTrue(where + ": 좋은 결과에 손해가 있음", !loss);
                assertTrue(where + ": 보상이 없음", item != null || stats.size() > 0);
                break;
            case BAD:
                assertTrue(where + ": 나쁜 결과에 손해가 없음", loss);
                break;
            case SPECIAL:
                assertTrue(where + ": 특별 선택지에 손해가 있음", !loss);
                assertTrue(where + ": 특별 선택지가 강화된 보상이 아님 (upper 장비나 120% 이상)",
                        ChaosTable.ITEM_UPPER.equals(item) || maxPercent >= 120);
                break;
        }
    }
}
