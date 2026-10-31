package com.textdungeon.scenario;

import com.textdungeon.billing.Entitlements;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 시나리오 목록, 해금 여부, 시나리오별 JSON 경로.
 * 로더는 경로를 직접 만들지 않고 반드시 여기서 받는다 (2단계에 '내려받은 시나리오' 출처를 더할 자리).
 */
public final class ScenarioRepository {

    /** 시나리오마다 있는 파일 이름. 기본 시나리오의 파일 이름과 같다 */
    public static final String EVENTS = "event_list.json";
    public static final String MONSTERS = "monster_list.json";
    public static final String ITEMS = "item_list.json";
    /** 혼돈의 주사위 결과표. 없으면 주사위는 온디바이스 모델로만 동작한다 */
    public static final String CHAOS = "chaos_table.json";
    /** 고를 수 있는 던전 (클리어 층·몬스터 배수·보상 배수) */
    public static final String DUNGEONS = "dungeon_list.json";

    public List<ScenarioPack> all() {
        return Collections.unmodifiableList(Arrays.asList(ScenarioPack.values()));
    }

    /** 고를 수 있는 시나리오가 둘 이상일 때만 선택 화면을 띄운다 */
    public boolean hasChoice() {
        return ScenarioPack.values().length > 1;
    }

    /** 무료이거나 보유한 시나리오 */
    public boolean isUnlocked(ScenarioPack pack, Entitlements entitlements) {
        return pack.product == null || entitlements.owns(pack.product);
    }

    /** 예: assetPath(BASE, EVENTS) = "event_list.json", DLC면 "scenarios/frost/event_list.json" */
    public String assetPath(ScenarioPack pack, String fileName) {
        return pack.assetDir.isEmpty() ? fileName : pack.assetDir + "/" + fileName;
    }
}
