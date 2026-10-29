package com.textdungeon.ai;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.textdungeon.event.BattleEvent;
import com.textdungeon.event.GameEvent;
import com.textdungeon.model.Item;
import com.textdungeon.model.RewardStat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 혼돈의 주사위 결과표 (시나리오마다 chaos_table.json).
 * 폰 모델이 없거나 실패했을 때 쓰는 일반 선택지와, 첫 굴림에서 일정 확률로 나오는 특별 선택지를 담는다.
 *
 *  - events: 이벤트 ID별 표. 없거나 다 쓴 이벤트는 types의 종류별 표(battle / normal)를 쓴다
 *  - 일반 선택지: 행동 하나에 좋은 결과(good)와 나쁜 결과(bad). 굴릴 때 둘 중 하나를 무작위로 고른다
 *  - 특별 선택지(specials): 항상 좋은 결과. 스탯은 층별 상한의 200%까지
 *  - stats 값은 층별 상한(RewardLimits)의 %. 음수면 깎는 쪽 하한의 %
 *  - item: "floor"면 그 층대 아이템, "upper"면 더 깊은 층의 장비 중 하나를 코드가 고른다
 */
public final class ChaosTable {
    private static final String TAG = "ChaosTable";
    public static final String ITEM_FLOOR = "floor";
    public static final String ITEM_UPPER = "upper";
    static final int NORMAL_LIMIT_PERCENT = 100;
    static final int SPECIAL_LIMIT_PERCENT = 200;

    public static final ChaosTable EMPTY = new ChaosTable();

    /** 결과 하나. 특별 선택지는 choice도 같이 적는다 */
    static final class Outcome {
        String choice;
        String text;
        String item;
        Map<String, Integer> stats;
    }

    /** 일반 선택지: 행동 하나에 좋은 결과와 나쁜 결과 */
    static final class Entry {
        String choice;
        Outcome good;
        Outcome bad;
    }

    static final class Section {
        List<Entry> choices;
        List<Outcome> specials;
    }

    private Map<String, Section> types;
    private Map<String, Section> events;

    /** 파일이 없거나 읽지 못하면 빈 표 (주사위는 모델로만 동작) */
    public static ChaosTable load(Context context, String assetPath) {
        try (Reader reader = new InputStreamReader(context.getAssets().open(assetPath), StandardCharsets.UTF_8)) {
            return parse(reader);
        } catch (IOException | RuntimeException e) {
            Log.w(TAG, "결과표를 읽지 못함: " + assetPath, e);
            return EMPTY;
        }
    }

    static ChaosTable parse(Reader reader) {
        ChaosTable table = new Gson().fromJson(reader, ChaosTable.class);
        return table != null ? table : EMPTY;
    }

    /** 일반 선택지 하나. 이벤트에 이미 있는 선택지와 같은 문장은 고르지 않는다. 고를 것이 없으면 null */
    public ChoiceDelta normal(GameEvent event, int floor, List<Item> nearItems, List<Item> upperItems, Random random) {
        Set<String> existing = existingChoices(event);
        for (Section section : sectionsFor(event)) {
            List<Entry> pool = new ArrayList<>();
            if (section.choices != null) {
                for (Entry e : section.choices) {
                    if (e != null && e.choice != null && e.good != null && e.bad != null
                            && !existing.contains(e.choice)) pool.add(e);
                }
            }
            if (pool.isEmpty()) continue;
            Entry entry = pool.get(random.nextInt(pool.size()));
            Outcome outcome = random.nextBoolean() ? entry.good : entry.bad;
            return toDelta(entry.choice, outcome, floor, nearItems, upperItems, random, NORMAL_LIMIT_PERCENT);
        }
        return null;
    }

    /** 특별 선택지 하나. 없으면 null */
    public ChoiceDelta special(GameEvent event, int floor, List<Item> nearItems, List<Item> upperItems, Random random) {
        Set<String> existing = existingChoices(event);
        for (Section section : sectionsFor(event)) {
            List<Outcome> pool = new ArrayList<>();
            if (section.specials != null) {
                for (Outcome o : section.specials) {
                    if (o != null && o.choice != null && !existing.contains(o.choice)) pool.add(o);
                }
            }
            if (pool.isEmpty()) continue;
            Outcome outcome = pool.get(random.nextInt(pool.size()));
            return toDelta(outcome.choice, outcome, floor, nearItems, upperItems, random, SPECIAL_LIMIT_PERCENT);
        }
        return null;
    }

    /** 이벤트별 표를 먼저, 그다음 종류별 표 */
    private List<Section> sectionsFor(GameEvent event) {
        List<Section> result = new ArrayList<>(2);
        if (events != null && event.getId() != null) {
            Section s = events.get(event.getId());
            if (s != null) result.add(s);
        }
        if (types != null) {
            Section s = types.get(typeKey(event));
            if (s != null) result.add(s);
        }
        return result;
    }

    static String typeKey(GameEvent event) {
        return event instanceof BattleEvent ? "battle" : "normal";
    }

    private static Set<String> existingChoices(GameEvent event) {
        return event.getChoices() == null ? Collections.emptySet() : new HashSet<>(event.getChoices());
    }

    private static ChoiceDelta toDelta(String choice, Outcome outcome, int floor, List<Item> nearItems,
                                       List<Item> upperItems, Random random, int limitPercent) {
        List<RewardStat> stats = new ArrayList<>();
        if (outcome.stats != null) {
            for (Map.Entry<String, Integer> s : outcome.stats.entrySet()) {
                if (s.getKey() == null || s.getValue() == null) continue;
                int value = RewardLimits.scaled(s.getKey(), s.getValue(), floor, limitPercent);
                if (value != 0) stats.add(new RewardStat(s.getKey(), value));
            }
        }
        return new ChoiceDelta(choice, outcome.text, pickItem(outcome.item, nearItems, upperItems, random), stats);
    }

    private static String pickItem(String kind, List<Item> nearItems, List<Item> upperItems, Random random) {
        List<Item> pool = ITEM_UPPER.equals(kind) ? upperItems : ITEM_FLOOR.equals(kind) ? nearItems : null;
        if (pool == null || pool.isEmpty()) return null;
        Item item = pool.get(random.nextInt(pool.size()));
        return item != null ? item.getId() : null;
    }
}
