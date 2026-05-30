package com.textdungeon.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.textdungeon.player.MagicScroll;

import org.junit.Before;
import org.junit.Test;

// ══════════════════════════════════════════════
//  MagicScroll 테스트
// ══════════════════════════════════════════════
public class MagicScrollTest {

    private MagicScroll scroll;

    @Before
    public void setUp() {
        scroll = new MagicScroll();
    }

    @Test
    public void addMagic_새_마법_추가() {
        scroll.addMagic("fireball", 3);
        assertTrue(scroll.hasMagic("fireball"));
    }

    @Test
    public void addMagic_중복_추가_무시() {
        scroll.addMagic("fireball", 3);
        scroll.addMagic("fireball", 5); // 중복
        // 리스트 크기는 1이어야 함
        assertEquals(1, scroll.getLearnedMagics().size());
    }

    @Test
    public void getMagic_존재하는_마법_반환() {
        scroll.addMagic("fireball", 3);
        LearnedMagic lm = scroll.getMagic("fireball");
        assertNotNull(lm);
        assertEquals("fireball", lm.getMagicId());
    }

    @Test
    public void getMagic_없는_마법은_null반환() {
        LearnedMagic lm = scroll.getMagic("nonexistent");
        assertNull(lm);
    }

    @Test
    public void removeMagic_정상_제거() {
        scroll.addMagic("fireball", 3);
        scroll.removeMagic("fireball");
        assertFalse(scroll.hasMagic("fireball"));
    }

    @Test
    public void restoreAll_모든_마법_count_복구() {
        scroll.addMagic("fireball", 3);
        scroll.addMagic("ice", 2);
        scroll.getMagic("fireball").use();
        scroll.getMagic("ice").use();
        scroll.getMagic("ice").use();

        scroll.restoreAll();

        assertEquals(3, scroll.getMagic("fireball").getCurrentCount());
        assertEquals(2, scroll.getMagic("ice").getCurrentCount());
    }

    @Test
    public void updateCounts_wisdom에_따라_count_갱신() {
        scroll.addMagic("fireball", 3);
        // wisdom=4 → addCount = 1 + (4/2) = 3
        scroll.updateCounts(4);
        assertEquals(3, scroll.getMagic("fireball").getCurrentCount());
        assertEquals(3, scroll.getMagic("fireball").getMaxCount());
    }

    @Test
    public void updateCounts_wisdom이_0이면_count는_1() {
        scroll.addMagic("fireball", 3);
        scroll.updateCounts(0);
        // addCount = 1 + (0/2) = 1
        assertEquals(1, scroll.getMagic("fireball").getCurrentCount());
    }
}
