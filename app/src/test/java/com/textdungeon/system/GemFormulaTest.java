package com.textdungeon.system;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

// ══════════════════════════════════════════════
//  GemFormula 테스트: (기본 + 클리어 보너스) × 부스터 × 광고
// ══════════════════════════════════════════════
public class GemFormulaTest {

    @Test
    public void 부스터_없고_광고_없으면_그대로() {
        assertEquals(1000, GemFormula.finalGems(1000, 1, false));
    }

    @Test
    public void 부스터_4배에_광고_2배면_8배() {
        assertEquals(8000, GemFormula.finalGems(1000, 4, true));
    }

    @Test
    public void 광고_추가분은_광고_뺀_값과의_차이() {
        assertEquals(3000, GemFormula.adBonusGems(1000, 3));
    }

    @Test
    public void 음수는_0() {
        assertEquals(0, GemFormula.finalGems(-50, 2, true));
    }

    @Test
    public void int를_넘으면_최댓값에서_멈춘다() {
        assertEquals(Integer.MAX_VALUE, GemFormula.finalGems(Integer.MAX_VALUE, 4, true));
    }
}
