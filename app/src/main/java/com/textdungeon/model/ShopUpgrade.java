package com.textdungeon.model;

public enum ShopUpgrade {
    // 항목: ID("한글이름", 최대레벨, 기본가격, 레벨당 가격 증가량,증가시킬 카테고리)
    //private int str, agi, health, wis;//힘,민첩,체력,지혜
    //private int statPoint, gold,diceChane;
    INIT_STAT_BONUS("초기 여유 스탯 추가", 5, 500, 500, "STAT_POINT", 1),
    INIT_STAT_STRENGTH("초기 힘 스탯 추가", 10, 100, 300, "STR", 2),
    INIT_STAT_AGILITY("초기 민첩 스탯 추가", 10, 100, 300, "AGI", 2),
    INIT_STAT_HEALTH("초기 체력 스탯 추가", 10, 100, 300, "HEALTH", 2),
    INIT_STAT_WISDOM("초기 지혜 스탯 추가", 10, 100, 300, "WIS", 2),
    INIT_GOLD_BONUS("초기 소지 골드 증가", 10, 300, 100, "GOLD", 100),
    INIT_DICE_LIMIT("혼돈의 주사위 사용횟수", 3, 1000, 500, "DICE_Chane", 1);

    public final String title;
    public final int maxLevel;
    public final int basePrice;
    public final int priceIncreasePerLevel;
    public final int valuePerLevel;
    public final String category;

    ShopUpgrade(String title, int maxLevel, int basePrice, int priceIncreasePerLevel, String category,int valuePerLevel) {
        this.title = title;
        this.category = category;
        this.maxLevel = maxLevel;
        this.basePrice = basePrice;
        this.priceIncreasePerLevel = priceIncreasePerLevel;
        this.valuePerLevel = valuePerLevel;
    }
    public int getNextPrice(int currentLevel) {
        return basePrice + (currentLevel * priceIncreasePerLevel);
    }
}