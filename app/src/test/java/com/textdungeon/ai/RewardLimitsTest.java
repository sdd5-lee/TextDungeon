package com.textdungeon.ai;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

// ══════════════════════════════════════════════
//  RewardLimits 테스트
//  clamp: AI가 만든 수치를 층별 상한으로 자른다 (기존 동작 유지)
//  scaled: 결과표의 %를 층별 상한에 맞춘 실제 수치로 바꾼다
// ══════════════════════════════════════════════
public class RewardLimitsTest {

    @Test
    public void clamp는_층별_상한과_하한으로_자른다() {
        assertEquals(45, RewardLimits.clamp("힘", 999, 40));
        assertEquals(-12, RewardLimits.clamp("힘", -999, 40));
        assertEquals(0, RewardLimits.clamp("경험치", -50, 10));
        assertEquals(23, RewardLimits.clamp("데미지", -50, 1)); // 데미지는 크기만, 1층 상한 23
        assertEquals(0, RewardLimits.clamp("행운", 10, 10));
    }

    @Test
    public void scaled는_상한의_퍼센트만큼() {
        assertEquals(8, RewardLimits.scaled("힘", 50, 10, 100));       // 10층 상한 15의 50% = 7.5 → 8
        assertEquals(500, RewardLimits.scaled("경험치", 100, 20, 100)); // 100 + 20×20
        assertEquals(25, RewardLimits.scaled("데미지", 50, 10, 100));   // (20 + 30)의 50%
    }

    @Test
    public void scaled_음수는_하한의_퍼센트만큼() {
        assertEquals(-12, RewardLimits.scaled("힘", -100, 40, 100));
        assertEquals(-75, RewardLimits.scaled("골드", -50, 10, 100)); // -(50 + 100)의 50%
        assertEquals(0, RewardLimits.scaled("경험치", -50, 10, 100)); // 경험치는 깎지 않는다
    }

    @Test
    public void scaled_회복과_데미지는_부호를_무시한다() {
        assertEquals(25, RewardLimits.scaled("데미지", -50, 10, 100));
        assertEquals(50, RewardLimits.scaled("회복", -50, 10, 100)); // (50 + 50)의 50%
    }

    @Test
    public void scaled는_허용_퍼센트를_넘지_않는다() {
        assertEquals(15, RewardLimits.scaled("힘", 300, 10, 100));
        assertEquals(30, RewardLimits.scaled("힘", 300, 10, 200));
        assertEquals(-4, RewardLimits.scaled("힘", -300, 10, 100)); // 10층 하한 -(2 + 2)
    }

    @Test
    public void scaled_모르는_종류는_0() {
        assertEquals(0, RewardLimits.scaled("행운", 100, 10, 100));
    }
}
