package com.textdungeon.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

// ══════════════════════════════════════════════
//  아이템 등급 (보이는 것만: 칸 테두리·이름 색)
//  - item_list.json의 "grade"에 이름으로 적는다. 없거나 모르는 값이면 일반
// ══════════════════════════════════════════════
public class ItemGradeTest {

    @Test
    public void 적힌_등급을_읽는다() {
        assertEquals(ItemGrade.EPIC, Item.createFromJson("{\"id\":\"item_x\",\"grade\":\"EPIC\"}").getGrade());
        assertEquals(ItemGrade.LEGENDARY, Item.createFromJson("{\"id\":\"item_x\",\"grade\":\"LEGENDARY\"}").getGrade());
    }

    @Test
    public void 등급이_없거나_모르는_값이면_일반() {
        assertEquals(ItemGrade.COMMON, Item.createFromJson("{\"id\":\"item_x\"}").getGrade());
        assertEquals(ItemGrade.COMMON, Item.createFromJson("{\"id\":\"item_x\",\"grade\":\"MYTHIC\"}").getGrade());
    }

    @Test
    public void 등급은_다섯이고_이름과_색이_모두_다르다() {
        ItemGrade[] g = ItemGrade.values();
        assertEquals(5, g.length);
        assertEquals("일반", g[0].displayName);
        assertEquals("전설", g[4].displayName);
        for (int i = 0; i < g.length; i++) {
            assertEquals(0xFF, g[i].color >>> 24);
            for (int j = i + 1; j < g.length; j++) {
                assertNotEquals(g[i].displayName, g[j].displayName);
                assertNotEquals(g[i].color, g[j].color);
            }
        }
    }
}
