package com.textdungeon.system;

import com.textdungeon.model.MonsterPattern;

import java.util.List;
import java.util.Random;

/**
 * 적이 이번 턴에 평타를 칠지 기술을 쓸지 고른다.
 * - 일반 몬스터(기술 1개): 평타 2 : 기술 1
 * - 보스(기술 2개): 체력이 절반보다 많으면 평타·첫 기술 1/2씩, 절반 이하가 되면 평타·첫 기술·두 번째 기술 1/3씩
 */
public final class EnemyAi {

    private EnemyAi() {}

    /** @return 쓸 기술. null이면 평타 */
    public static MonsterPattern choose(List<MonsterPattern> patterns, int hp, int maxHp, Random random) {
        if (patterns == null || patterns.isEmpty()) return null;
        if (patterns.size() == 1) {
            return random.nextInt(3) == 0 ? patterns.get(0) : null;
        }
        if (!isEnraged(hp, maxHp)) {
            return random.nextInt(2) == 0 ? patterns.get(0) : null;
        }
        int pick = random.nextInt(3);
        return pick == 0 ? null : patterns.get(pick - 1);
    }

    /** 보스가 두 번째 기술을 꺼내는 체력 (절반 이하) */
    public static boolean isEnraged(int hp, int maxHp) {
        return hp * 2 <= maxHp;
    }
}
