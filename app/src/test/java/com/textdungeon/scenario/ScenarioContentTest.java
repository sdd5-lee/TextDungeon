package com.textdungeon.scenario;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

// ══════════════════════════════════════════════
//  시나리오 콘텐츠 검사 (assets 파일을 직접 읽는다)
//  - 시나리오끼리 아이템·몬스터·이벤트 ID가 겹치면 도감(발견 목록)이 섞이므로 막는다
//  - ScenarioPack에 적힌 DLC 폴더에 네 파일(이벤트·몬스터·아이템·던전)이 다 있는지
// ══════════════════════════════════════════════
public class ScenarioContentTest {

    private static final File ASSETS = new File("src/main/assets");
    private static final String[] FILES = {
            ScenarioRepository.EVENTS, ScenarioRepository.MONSTERS, ScenarioRepository.ITEMS, ScenarioRepository.DUNGEONS};

    @Test
    public void DLC_폴더에_네_파일이_다_있다() {
        for (ScenarioPack p : ScenarioPack.values()) {
            if (p.assetDir.isEmpty()) continue;
            for (String f : FILES) {
                File file = new File(ASSETS, p.assetDir + "/" + f);
                assertTrue("없음: " + file, file.isFile());
            }
        }
    }

    @Test
    public void 시나리오끼리_ID가_겹치지_않는다() throws Exception {
        assertTrue("assets 폴더를 못 찾음 (작업 폴더가 app 모듈이어야 함)", ASSETS.isDirectory());
        // 기본 시나리오 + assets/scenarios/ 아래 모든 폴더 (ScenarioPack에 아직 안 적은 폴더도 검사)
        Map<String, File> dirs = new HashMap<>();
        dirs.put("base", ASSETS);
        File[] dlcs = new File(ASSETS, "scenarios").listFiles(File::isDirectory);
        if (dlcs != null) {
            for (File d : dlcs) dirs.put(d.getName(), d);
        }

        for (String f : FILES) {
            Map<String, String> owner = new HashMap<>();
            for (Map.Entry<String, File> dir : dirs.entrySet()) {
                File file = new File(dir.getValue(), f);
                if (!file.isFile()) continue;
                try (Reader r = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
                    for (JsonElement e : JsonParser.parseReader(r).getAsJsonArray()) {
                        String id = e.getAsJsonObject().get("id").getAsString();
                        String prev = owner.put(id, dir.getKey());
                        if (prev != null && !prev.equals(dir.getKey())) {
                            fail(f + "의 ID '" + id + "'가 " + prev + "와 " + dir.getKey() + " 시나리오에 모두 있음");
                        }
                    }
                }
            }
        }
    }
}
