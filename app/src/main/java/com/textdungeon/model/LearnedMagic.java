package com.textdungeon.model;

/**
 * 배운 마법. 사용 횟수는 없어지고 싸움마다 쿨타임(BattleSystem의 MagicCooldowns)으로 바뀌었다.
 * 예전 세이브의 currentCount·maxCount는 Gson이 무시한다.
 */
public class LearnedMagic {
    private String magicId;

    public LearnedMagic(String magicId) {
        this.magicId = magicId;
    }

    public String getMagicId() {
        return magicId;
    }
}
