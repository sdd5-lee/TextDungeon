package com.textdungeon.system;

import java.util.HashMap;
import java.util.Map;

/**
 * 한 싸움 동안의 마법 쿨타임. 싸움을 시작하면 모든 마법이 준비돼 있고, 세이브에는 남지 않는다.
 * 내 행동(공격·방어·도망 실패·마법)이 하나 끝날 때마다 한 턴이 지난다.
 * 쿨타임 2인 마법을 1턴에 쓰면 2·3턴엔 쉬고 4턴에 다시 쓸 수 있다.
 */
public class MagicCooldowns {

    private final Map<String, Integer> left = new HashMap<>();

    /** 다시 쓰기까지 남은 내 턴 수 (0이면 지금 쓸 수 있다) */
    public int left(String magicId) {
        Integer turns = left.get(magicId);
        return turns != null ? turns : 0;
    }

    public boolean isReady(String magicId) {
        return left(magicId) <= 0;
    }

    /** 내 행동을 시작할 때 부른다. 이번 행동으로 쓴 마법은 그 뒤에 use()로 다시 채운다 */
    public void passTurn() {
        for (Map.Entry<String, Integer> e : left.entrySet()) {
            e.setValue(Math.max(0, e.getValue() - 1));
        }
    }

    public void use(String magicId, int cooldown) {
        left.put(magicId, Math.max(0, cooldown));
    }
}
