package com.textdungeon.model;

public class RewardStat {
    private String type;
    private int value;

    /** Gson 역직렬화용 기본 생성자 */
    public RewardStat() {}

    public RewardStat(String type, int value) {
        this.type = type;
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public String getStatType() {
        return type;
    }
}
