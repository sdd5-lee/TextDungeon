package com.textdungeon.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

// ══════════════════════════════════════════════
//  MagicCooldowns 테스트
//  BattleSystem은 내 행동마다 passTurn()을 먼저 부르고, 마법을 쓰면 그 뒤에 use()를 부른다
// ══════════════════════════════════════════════
public class MagicCooldownsTest {

    private MagicCooldowns cooldowns;

    @Before
    public void setUp() {
        cooldowns = new MagicCooldowns();
    }

    /** 내 턴 하나: 턴이 지나고, 마법을 쓰면 쿨타임을 건다 */
    private void myTurn(String castId, int cooldown) {
        cooldowns.passTurn();
        if (castId != null) cooldowns.use(castId, cooldown);
    }

    @Test
    public void 싸움을_시작하면_모든_마법이_준비() {
        assertTrue(cooldowns.isReady("mag_1"));
        assertEquals(0, cooldowns.left("mag_1"));
    }

    @Test
    public void 쿨타임_1은_한_턴_쉬고_다시_쓴다() {
        myTurn("mag_1", 1);                       // 1턴: 매직 미사일
        assertFalse(cooldowns.isReady("mag_1"));  // 2턴엔 못 쓴다
        assertEquals(1, cooldowns.left("mag_1"));
        myTurn(null, 0);                          // 2턴: 공격
        assertTrue(cooldowns.isReady("mag_1"));   // 3턴엔 쓴다
    }

    @Test
    public void 쿨타임_5는_다섯_턴_쉬고_여섯째_턴_뒤에_쓴다() {
        myTurn("mag_15", 5); // 1턴
        for (int turn = 2; turn <= 6; turn++) {
            assertFalse(turn + "턴에 벌써 쓸 수 있다", cooldowns.isReady("mag_15"));
            assertEquals(7 - turn, cooldowns.left("mag_15"));
            myTurn(null, 0);
        }
        assertTrue(cooldowns.isReady("mag_15")); // 7턴
    }

    @Test
    public void 다른_마법을_써도_각자_쿨타임이_줄어든다() {
        myTurn("mag_2", 2);           // 1턴: 화염구
        myTurn("mag_1", 1);           // 2턴: 매직 미사일
        assertEquals(1, cooldowns.left("mag_2"));
        assertEquals(1, cooldowns.left("mag_1"));
        myTurn(null, 0);              // 3턴: 방어
        assertTrue(cooldowns.isReady("mag_2"));
        assertTrue(cooldowns.isReady("mag_1"));
    }
}
