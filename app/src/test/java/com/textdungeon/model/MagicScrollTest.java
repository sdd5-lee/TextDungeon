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
        scroll.addMagic("fireball");
        assertTrue(scroll.hasMagic("fireball"));
    }

    @Test
    public void addMagic_중복_추가_무시() {
        scroll.addMagic("fireball");
        scroll.addMagic("fireball"); // 중복
        // 리스트 크기는 1이어야 함
        assertEquals(1, scroll.getLearnedMagics().size());
    }

    @Test
    public void getMagic_존재하는_마법_반환() {
        scroll.addMagic("fireball");
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
        scroll.addMagic("fireball");
        scroll.removeMagic("fireball");
        assertFalse(scroll.hasMagic("fireball"));
    }

    // ── 코어 포인트 ────────────────────────────────────────────

    @Test
    public void 마법사_계열은_코어_1개로_시작() {
        scroll.startRun(1, 12);
        assertEquals(1, scroll.getTotalCores(12));
        assertEquals(1, scroll.getAvailableCores(12));
        assertEquals(20, scroll.getNextCoreWisdom(12));
    }

    @Test
    public void 다른_직업은_지혜_20에서_첫_코어() {
        scroll.startRun(0, 2);
        assertEquals(0, scroll.getTotalCores(19));
        assertEquals(1, scroll.getTotalCores(20));
    }

    @Test
    public void 지혜_20마다_코어_하나씩() {
        scroll.startRun(1, 12);
        scroll.recordWisdom(45);
        assertEquals(3, scroll.getTotalCores(45)); // 시작 1 + 20 + 40
        assertEquals(60, scroll.getNextCoreWisdom(45));
    }

    @Test
    public void 지혜가_떨어져도_얻은_코어는_그대로() {
        scroll.startRun(0, 2);
        scroll.recordWisdom(41);
        assertEquals(2, scroll.getTotalCores(30));
        assertEquals(60, scroll.getNextCoreWisdom(30));
    }

    @Test
    public void 계열을_열면_코어를_하나_쓴다() {
        scroll.startRun(1, 12);
        assertTrue(scroll.openSchool(MagicSchool.FIRE, 12));
        assertTrue(scroll.isSchoolOpen(MagicSchool.FIRE));
        assertEquals(0, scroll.getAvailableCores(12));
        assertFalse(scroll.openSchool(MagicSchool.FROST, 12)); // 코어 없음
        assertFalse(scroll.isSchoolOpen(MagicSchool.FROST));
    }

    @Test
    public void 이미_연_계열은_다시_열지_않는다() {
        scroll.startRun(0, 2);
        scroll.recordWisdom(40);
        assertTrue(scroll.openSchool(MagicSchool.FIRE, 40));
        assertFalse(scroll.openSchool(MagicSchool.FIRE, 40));
        assertEquals(1, scroll.getAvailableCores(40));
    }

    @Test
    public void 기초_계열은_코어_없이_열려_있다() {
        scroll.startRun(0, 2);
        assertTrue(scroll.isSchoolOpen(MagicSchool.BASIC));
    }

    @Test
    public void 새_코어는_한_번만_알린다() {
        scroll.startRun(0, 2);
        assertEquals(0, scroll.takeNewCores(10));
        scroll.recordWisdom(20);
        assertEquals(1, scroll.takeNewCores(20));
        assertEquals(0, scroll.takeNewCores(20));
    }

    @Test
    public void 시작_코어는_알리지_않는다() {
        scroll.startRun(1, 12);
        assertEquals(0, scroll.takeNewCores(12));
    }

    // ── 배울 수 있는지 ────────────────────────────────────────────

    @Test
    public void 배우기_조건은_계열_이전마법_지혜_순서로_막힌다() {
        Magic missile = new Magic("mag_1", "매직 미사일", MagicSchool.BASIC, null, 5);
        Magic fireball = new Magic("mag_2", "화염구", MagicSchool.FIRE, "mag_1", 15);
        scroll.startRun(1, 12);

        assertEquals(MagicScroll.LearnState.SCHOOL_LOCKED, scroll.getLearnState(fireball, 12));
        scroll.openSchool(MagicSchool.FIRE, 12);
        assertEquals(MagicScroll.LearnState.NEED_PREVIOUS, scroll.getLearnState(fireball, 12));
        scroll.addMagic(missile.getId());
        assertEquals(MagicScroll.LearnState.NEED_WISDOM, scroll.getLearnState(fireball, 12));
        assertEquals(MagicScroll.LearnState.CAN_LEARN, scroll.getLearnState(fireball, 15));
        scroll.addMagic(fireball.getId());
        assertEquals(MagicScroll.LearnState.LEARNED, scroll.getLearnState(fireball, 15));
    }

    @Test
    public void 기초_마법은_코어_없이_지혜만_있으면_배운다() {
        Magic missile = new Magic("mag_1", "매직 미사일", MagicSchool.BASIC, null, 5);
        scroll.startRun(0, 2);
        assertEquals(MagicScroll.LearnState.NEED_WISDOM, scroll.getLearnState(missile, 4));
        assertEquals(MagicScroll.LearnState.CAN_LEARN, scroll.getLearnState(missile, 5));
    }
}
