package com.textdungeon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.textdungeon.data.Difficulty;

import org.junit.Test;

// ══════════════════════════════════════════════
//  Difficulty enum 테스트
// ══════════════════════════════════════════════
public class DifficultyTest {

    @Test
    public void EASY_statMultiplier는_1() {
        assertEquals(1, Difficulty.EASY.statMultiplier);
    }

    @Test
    public void NORMAL_statMultiplier는_2() {
        assertEquals(2, Difficulty.NORMAL.statMultiplier);
    }

    @Test
    public void HARD_statMultiplier는_3() {
        assertEquals(3, Difficulty.HARD.statMultiplier);
    }

    @Test
    public void 난이도가_높을수록_eventCount_증가() {
        assertTrue(Difficulty.EASY.eventCount < Difficulty.NORMAL.eventCount);
        assertTrue(Difficulty.NORMAL.eventCount < Difficulty.HARD.eventCount);
    }

    @Test
    public void 난이도가_높을수록_rewardMultiplier_증가() {
        assertTrue(Difficulty.EASY.rewardMultiplier < Difficulty.NORMAL.rewardMultiplier);
        assertTrue(Difficulty.NORMAL.rewardMultiplier < Difficulty.HARD.rewardMultiplier);
    }

    @Test
    public void 모든_값이_양수() {
        for (Difficulty d : Difficulty.values()) {
            assertTrue(d.statMultiplier > 0);
            assertTrue(d.rewardMultiplier > 0);
            assertTrue(d.eventCount > 0);
        }
    }
}
