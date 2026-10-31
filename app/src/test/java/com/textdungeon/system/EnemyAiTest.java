package com.textdungeon.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.textdungeon.model.MonsterPattern;

import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

// ══════════════════════════════════════════════
//  EnemyAi 테스트: 평타(null)와 기술 중 무엇을 고르는지
//  - 일반 몬스터(기술 1개): nextInt(3)이 0일 때만 기술 (1/3)
//  - 보스(기술 2개): 체력이 절반보다 많으면 nextInt(2)로 평타·첫 기술,
//    절반 이하면 nextInt(3)으로 평타·첫 기술·두 번째 기술
// ══════════════════════════════════════════════
public class EnemyAiTest {

    private static final MonsterPattern FIRST = new MonsterPattern("첫 기술", 150, 1, false);
    private static final MonsterPattern SECOND = new MonsterPattern("두 번째 기술", 300, 1, true);
    private static final List<MonsterPattern> BOSS = Arrays.asList(FIRST, SECOND);

    /** 정해 둔 값을 차례로 돌려주고, 매번 받은 범위를 기록한다 */
    private static class FixedRandom extends Random {
        final Deque<Integer> values = new ArrayDeque<>();
        final Deque<Integer> bounds = new ArrayDeque<>();

        FixedRandom(Integer... values) {
            this.values.addAll(Arrays.asList(values));
        }

        @Override
        public int nextInt(int bound) {
            bounds.add(bound);
            return values.poll();
        }
    }

    @Test
    public void 기술이_없으면_항상_평타() {
        assertNull(EnemyAi.choose(null, 10, 10, new FixedRandom()));
        assertNull(EnemyAi.choose(Collections.emptyList(), 10, 10, new FixedRandom()));
    }

    @Test
    public void 일반_몬스터는_셋_중_하나로_기술() {
        List<MonsterPattern> one = Collections.singletonList(FIRST);
        FixedRandom r = new FixedRandom(0, 1, 2);
        assertSame(FIRST, EnemyAi.choose(one, 100, 100, r));
        assertNull(EnemyAi.choose(one, 100, 100, r));
        assertNull(EnemyAi.choose(one, 100, 100, r));
        assertEquals(Arrays.asList(3, 3, 3), Arrays.asList(r.bounds.toArray()));
    }

    @Test
    public void 일반_몬스터는_체력이_낮아도_같은_확률() {
        List<MonsterPattern> one = Collections.singletonList(FIRST);
        FixedRandom r = new FixedRandom(0);
        assertSame(FIRST, EnemyAi.choose(one, 1, 100, r));
        assertEquals(Integer.valueOf(3), r.bounds.peek());
    }

    @Test
    public void 보스는_체력이_절반보다_많으면_평타와_첫_기술_반반() {
        FixedRandom r = new FixedRandom(0, 1);
        assertSame(FIRST, EnemyAi.choose(BOSS, 51, 100, r));
        assertNull(EnemyAi.choose(BOSS, 100, 100, r));
        assertEquals(Arrays.asList(2, 2), Arrays.asList(r.bounds.toArray()));
    }

    @Test
    public void 보스는_체력이_절반_이하면_평타와_두_기술_셋으로_나눈다() {
        FixedRandom r = new FixedRandom(0, 1, 2);
        assertNull(EnemyAi.choose(BOSS, 50, 100, r));
        assertSame(FIRST, EnemyAi.choose(BOSS, 50, 100, r));
        assertSame(SECOND, EnemyAi.choose(BOSS, 1, 100, r));
        assertEquals(Arrays.asList(3, 3, 3), Arrays.asList(r.bounds.toArray()));
    }

    @Test
    public void 분노는_체력이_정확히_절반부터() {
        assertFalse(EnemyAi.isEnraged(51, 100));
        assertTrue(EnemyAi.isEnraged(50, 100));
        assertFalse(EnemyAi.isEnraged(6, 11));
        assertTrue(EnemyAi.isEnraged(5, 11));
    }

    @Test
    public void 실제로_돌리면_확률이_맞다() {
        Random r = new Random(7);
        List<MonsterPattern> one = Collections.singletonList(FIRST);
        int n = 30000, normal = 0, calm = 0, enragedSecond = 0;
        for (int i = 0; i < n; i++) {
            if (EnemyAi.choose(one, 100, 100, r) != null) normal++;
            if (EnemyAi.choose(BOSS, 100, 100, r) != null) calm++;
            if (EnemyAi.choose(BOSS, 10, 100, r) == SECOND) enragedSecond++;
        }
        assertEquals(1 / 3.0, normal / (double) n, 0.02);
        assertEquals(1 / 2.0, calm / (double) n, 0.02);
        assertEquals(1 / 3.0, enragedSecond / (double) n, 0.02);
    }
}
