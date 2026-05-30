package com.textdungeon.model;

import com.textdungeon.player.MagicScroll;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

// ══════════════════════════════════════════════
//  LearnedMagic 테스트
// ══════════════════════════════════════════════
public class LearnedMagicTest {

    private LearnedMagic magic;

    @Before
    public void setUp() {
        magic = new LearnedMagic("fireball", 3);
    }

    @Test
    public void 초기_currentCount는_maxCount와_동일() {
        assertEquals(3, magic.getCurrentCount());
        assertEquals(3, magic.getMaxCount());
    }

    @Test
    public void use_성공시_true반환하고_count감소() {
        boolean result = magic.use();
        assertTrue(result);
        assertEquals(2, magic.getCurrentCount());
    }

    @Test
    public void use_count가_0이면_false반환() {
        magic.use();
        magic.use();
        magic.use();
        boolean result = magic.use(); // 4번째
        assertFalse(result);
        assertEquals(0, magic.getCurrentCount());
    }

    @Test
    public void use_count가_0일때_count가_음수가_되지_않음() {
        magic.use(); magic.use(); magic.use();
        magic.use(); // 초과 사용
        assertEquals(0, magic.getCurrentCount());
    }

    @Test
    public void restore_호출시_currentCount가_maxCount로_복구() {
        magic.use();
        magic.use();
        magic.restore();
        assertEquals(magic.getMaxCount(), magic.getCurrentCount());
    }

    @Test
    public void setCurrentCount_정상_동작() {
        magic.setCurrentCount(1);
        assertEquals(1, magic.getCurrentCount());
    }

    @Test
    public void setMaxCount_정상_동작() {
        magic.setMaxCount(5);
        assertEquals(5, magic.getMaxCount());
    }
}


