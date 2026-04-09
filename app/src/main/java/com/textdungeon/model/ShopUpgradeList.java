package com.textdungeon.model;

public enum ShopUpgradeList {
    // 항목: ID("한글이름", 최대레벨, 기본가격, 레벨당 가격 증가량)
    INIT_STAT_BONUS("초기 여유 스탯 추가", 5, 500, 200),
    INIT_GOLD_BONUS("초기 소지 골드 증가", 10, 300, 100);

    public final String name;
    public final int maxLevel;
    public final int basePrice;
    public final int priceIncreasePerLevel;

    ShopUpgradeList(String name, int maxLevel, int basePrice, int priceIncreasePerLevel) {
        this.name = name;
        this.maxLevel = maxLevel;
        this.basePrice = basePrice;
        this.priceIncreasePerLevel = priceIncreasePerLevel;
    }
    public int getNextPrice(int currentLevel) {
        return basePrice + (currentLevel * priceIncreasePerLevel);
    }
}