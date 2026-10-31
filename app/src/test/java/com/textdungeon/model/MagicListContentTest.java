package com.textdungeon.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// ══════════════════════════════════════════════
//  마법 나무 내용 검사 (assets/magic_list.json을 직접 읽는다)
//  - 기초 마법은 하나, 앞 마법 없이
//  - 나머지는 같은 계열이나 기초 마법을 앞 마법으로 가진다
//  - 갈래 깊이는 MagicTreeView가 그릴 수 있는 3단계까지
//  - 뒤 마법일수록 요구 지혜가 높다
//  - 단계마다 요구 지혜와 쿨타임이 정해져 있다 (기초 5·1턴, 1단계 15·2턴, 2단계 30·3턴, 3단계 40·5턴)
//  - 계열마다 첫 마법 하나에서 두 갈래로 갈라져 2·3단계는 두 개씩
//  - 마법사류(시작 코어가 있는 직업)는 시작하자마자 매직 미사일을 배울 수 있다
// ══════════════════════════════════════════════
public class MagicListContentTest {

    private static final int MAX_DEPTH = 3;
    /** 기초 마법에서 몇 단계 위인지(0 = 매직 미사일)에 따른 요구 지혜와 쿨타임 */
    private static final int[] WISDOM_BY_DEPTH = {5, 15, 30, 40};
    private static final int[] COOLDOWN_BY_DEPTH = {1, 2, 3, 5};
    private static final Map<String, JsonObject> magics = new HashMap<>();

    @BeforeClass
    public static void load() throws Exception {
        File file = new File("src/main/assets/magic_list.json");
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
            for (JsonElement e : array) {
                JsonObject o = e.getAsJsonObject();
                magics.put(o.get("id").getAsString(), o);
            }
        }
    }

    private static MagicSchool school(JsonObject o) {
        return MagicSchool.valueOf(o.get("school").getAsString());
    }

    private static String requires(JsonObject o) {
        return o.has("requires") ? o.get("requires").getAsString() : null;
    }

    @Test
    public void 기초_마법은_하나이고_앞_마법이_없다() {
        int basic = 0;
        for (JsonObject o : magics.values()) {
            if (school(o) == MagicSchool.BASIC) {
                basic++;
                assertEquals(o.get("id").getAsString(), null, requires(o));
            }
        }
        assertEquals(1, basic);
    }

    @Test
    public void 앞_마법은_같은_계열이거나_기초_마법() {
        for (JsonObject o : magics.values()) {
            if (school(o) == MagicSchool.BASIC) continue;
            String id = o.get("id").getAsString();
            JsonObject prev = magics.get(requires(o));
            assertNotNull(id + "의 앞 마법이 없다", prev);
            MagicSchool ps = school(prev);
            assertTrue(id + "의 앞 마법 계열이 다르다", ps == school(o) || ps == MagicSchool.BASIC);
        }
    }

    @Test
    public void 갈래는_3단계까지이고_돌지_않는다() {
        for (JsonObject o : magics.values()) {
            int depth = 0;
            for (String req = requires(o); req != null; req = requires(magics.get(req))) {
                depth++;
                assertFalse(o.get("id").getAsString() + " 갈래가 너무 깊거나 돈다", depth > MAX_DEPTH);
            }
        }
    }

    @Test
    public void 뒤_마법일수록_요구_지혜가_높다() {
        for (JsonObject o : magics.values()) {
            String req = requires(o);
            if (req == null) continue;
            assertTrue(o.get("id").getAsString() + " 요구 지혜가 앞 마법보다 낮다",
                    o.get("value").getAsInt() > magics.get(req).get("value").getAsInt());
        }
    }

    private static int depth(JsonObject o) {
        int depth = 0;
        for (String req = requires(o); req != null; req = requires(magics.get(req))) depth++;
        return depth;
    }

    @Test
    public void 단계마다_요구_지혜와_쿨타임이_정해져_있다() {
        for (JsonObject o : magics.values()) {
            String id = o.get("id").getAsString();
            int depth = depth(o);
            assertEquals(id + " 요구 지혜", WISDOM_BY_DEPTH[depth], o.get("value").getAsInt());
            assertEquals(id + " 쿨타임", COOLDOWN_BY_DEPTH[depth], o.get("cooldown").getAsInt());
            assertTrue(id + " 지혜 비율이 없다", o.get("wisdomPercent").getAsInt() > 0);
        }
    }

    @Test
    public void 계열마다_첫_마법_하나에서_두_갈래로_갈라진다() {
        for (MagicSchool school : MagicSchool.values()) {
            if (!school.needsCore()) continue;
            int[] perDepth = new int[MAX_DEPTH + 1];
            for (JsonObject o : magics.values()) {
                if (school(o) == school) perDepth[depth(o)]++;
            }
            assertEquals(school + " 1단계", 1, perDepth[1]);
            assertEquals(school + " 2단계", 2, perDepth[2]);
            assertEquals(school + " 3단계", 2, perDepth[3]);
        }
        // 3단계는 2단계마다 하나씩 (한 2단계 아래에 둘이 몰리지 않게)
        Map<String, Integer> children = new HashMap<>();
        for (JsonObject o : magics.values()) {
            if (depth(o) == 3) children.merge(requires(o), 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> e : children.entrySet()) {
            assertEquals(e.getKey() + " 아래 3단계 수", 1, (int) e.getValue());
        }
    }

    @Test
    public void 마법사류는_시작_지혜로_매직_미사일을_배운다() {
        for (Job job : Job.values()) {
            if (!job.startsWithMagicCore()) continue;
            for (JsonObject o : magics.values()) {
                if (requires(o) != null) continue;
                assertTrue(job.name + "이(가) 시작 지혜로 " + o.get("name").getAsString() + "을(를) 못 배운다",
                        job.wisdom >= o.get("value").getAsInt());
            }
        }
    }

    @Test
    public void 모든_계열에_마법이_있다() {
        Set<MagicSchool> found = EnumSet.noneOf(MagicSchool.class);
        for (JsonObject o : magics.values()) found.add(school(o));
        assertEquals(EnumSet.allOf(MagicSchool.class), found);
    }
}
