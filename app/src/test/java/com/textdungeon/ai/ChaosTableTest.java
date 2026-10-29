package com.textdungeon.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.gson.Gson;
import com.textdungeon.event.BattleEvent;
import com.textdungeon.event.GameEvent;
import com.textdungeon.model.Item;
import com.textdungeon.model.RewardStat;

import org.junit.Test;

import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// ══════════════════════════════════════════════
//  ChaosTable 테스트
//  이벤트별 표 → 종류별 표 순서, 기존 선택지 제외, 수치 환산(일반 100% / 특별 200%), 아이템 고르기
// ══════════════════════════════════════════════
public class ChaosTableTest {

    private static final String JSON = "{"
            + "\"types\":{"
            + "  \"normal\":{\"choices\":[" + entry("일반 공용") + "],"
            + "    \"specials\":[{\"choice\":\"일반 특별\",\"text\":\"특별.\",\"stats\":{\"힘\":300}}]},"
            + "  \"battle\":{\"choices\":[" + entry("전투 공용") + "]}"
            + "},"
            + "\"events\":{"
            + "  \"event_0\":{\"choices\":[" + entry("샘 전용") + "],"
            + "    \"specials\":[{\"choice\":\"샘 특별\",\"text\":\"샘 특별.\",\"item\":\"upper\",\"stats\":{\"힘\":150}}]}"
            + "}}";

    private static String entry(String choice) {
        return "{\"choice\":\"" + choice + "\","
                + "\"good\":{\"text\":\"좋다.\",\"item\":\"floor\",\"stats\":{\"힘\":150,\"경험치\":50}},"
                + "\"bad\":{\"text\":\"나쁘다.\",\"stats\":{\"데미지\":50}}}";
    }

    private final ChaosTable table = ChaosTable.parse(new StringReader(JSON));

    /** 목록에서는 항상 첫 번째, 결과는 nextBoolean 값으로 */
    private static Random fixed(boolean good) {
        return new Random() {
            @Override
            public int nextInt(int bound) {
                return 0;
            }

            @Override
            public boolean nextBoolean() {
                return good;
            }
        };
    }

    private static GameEvent event(String id, String... choices) {
        String list = choices.length == 0 ? "" : "\"" + String.join("\",\"", choices) + "\"";
        return new Gson().fromJson("{\"id\":\"" + id + "\",\"choices\":[" + list + "]}", GameEvent.class);
    }

    private static List<Item> items(String id) {
        return Collections.singletonList(Item.createFromJson(
                "{\"id\":\"" + id + "\",\"name\":\"" + id + "\",\"type\":\"weapon\"}"));
    }

    private static int stat(ChoiceDelta d, String type) {
        for (RewardStat s : d.getStats()) if (type.equals(s.getStatType())) return s.getValue();
        return 0;
    }

    @Test
    public void 이벤트별_표가_있으면_먼저_쓴다() {
        ChoiceDelta d = table.normal(event("event_0"), 10, items("near"), items("upper"), fixed(true));
        assertEquals("샘 전용", d.getChoiceText());
    }

    @Test
    public void 이벤트별_표를_다_쓰면_종류별_표로() {
        ChoiceDelta d = table.normal(event("event_0", "샘 전용"), 10, items("near"), items("upper"), fixed(true));
        assertEquals("일반 공용", d.getChoiceText());
    }

    @Test
    public void 표에_없는_이벤트는_종류별_표() {
        assertEquals("일반 공용", table.normal(event("ai_event"), 10, items("near"), items("upper"), fixed(true)).getChoiceText());
        GameEvent battle = new Gson().fromJson("{\"id\":\"ai_battle\",\"choices\":[]}", BattleEvent.class);
        assertEquals("전투 공용", table.normal(battle, 10, items("near"), items("upper"), fixed(true)).getChoiceText());
    }

    @Test
    public void 고를_것이_없으면_null() {
        assertNull(table.normal(event("ai_event", "일반 공용"), 10, items("near"), items("upper"), fixed(true)));
        assertNull(ChaosTable.EMPTY.normal(event("event_0"), 10, items("near"), items("upper"), fixed(true)));
    }

    @Test
    public void 일반_선택지는_상한의_100퍼센트까지() {
        ChoiceDelta good = table.normal(event("ai_event"), 10, items("near"), items("upper"), fixed(true));
        assertEquals(15, stat(good, "힘"));      // 150%를 100%로 자름: 10층 상한 15
        assertEquals(150, stat(good, "경험치")); // (100 + 200)의 50%
        assertEquals("near", good.getItemId());  // "floor"는 이 층대 아이템

        ChoiceDelta bad = table.normal(event("ai_event"), 10, items("near"), items("upper"), fixed(false));
        assertEquals("나쁘다.", bad.getRewardDescription());
        assertEquals(25, stat(bad, "데미지"));
        assertNull(bad.getItemId());
    }

    @Test
    public void 특별_선택지는_상한의_200퍼센트까지() {
        ChoiceDelta common = table.special(event("ai_event"), 10, items("near"), items("upper"), fixed(true));
        assertEquals("일반 특별", common.getChoiceText());
        assertEquals(30, stat(common, "힘")); // 300%를 200%로 자름

        ChoiceDelta own = table.special(event("event_0"), 10, items("near"), items("upper"), fixed(true));
        assertEquals("샘 특별", own.getChoiceText());
        assertEquals(23, stat(own, "힘"));    // 15의 150% = 22.5 → 23
        assertEquals("upper", own.getItemId()); // "upper"는 깊은 층 장비
    }

    @Test
    public void 종류별_표에_특별이_없으면_null() {
        GameEvent battle = new Gson().fromJson("{\"id\":\"ai_battle\",\"choices\":[]}", BattleEvent.class);
        assertNull(table.special(battle, 10, items("near"), items("upper"), fixed(true)));
    }
}
