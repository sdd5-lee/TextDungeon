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
// ══════════════════════════════════════════════
public class MagicListContentTest {

    private static final int MAX_DEPTH = 3;
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

    @Test
    public void 모든_계열에_마법이_있다() {
        Set<MagicSchool> found = EnumSet.noneOf(MagicSchool.class);
        for (JsonObject o : magics.values()) found.add(school(o));
        assertEquals(EnumSet.allOf(MagicSchool.class), found);
    }
}
