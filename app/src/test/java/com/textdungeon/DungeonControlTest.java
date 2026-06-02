package com.textdungeon;

import static org.junit.Assert.assertEquals;

import com.textdungeon.data.DungeonControl;

import org.junit.Before;
import org.junit.Test;

// ══════════════════════════════════════════════
//  DungeonControl 테스트
// ══════════════════════════════════════════════
public class DungeonControlTest {

    private DungeonControl dc;

    @Before
    public void setUp() {
        dc = new DungeonControl();
    }

    @Test
    public void 초기_층수는_1() {
        assertEquals(1, dc.getCurrentFloor());
    }

    @Test
    public void nextCurrentFloor_호출시_1_증가() {
        dc.nextCurrentFloor();
        assertEquals(2, dc.getCurrentFloor());
    }

    @Test
    public void nextCurrentFloor_여러번_호출() {
        dc.nextCurrentFloor();
        dc.nextCurrentFloor();
        dc.nextCurrentFloor();
        assertEquals(4, dc.getCurrentFloor());
    }

    @Test
    public void setCurrentFloor_직접_설정() {
        dc.setCurrentFloor(25);
        assertEquals(25, dc.getCurrentFloor());
    }
}
