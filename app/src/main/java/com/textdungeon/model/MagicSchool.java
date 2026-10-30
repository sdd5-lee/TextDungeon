package com.textdungeon.model;

/**
 * 마법 계열. BASIC(매직 미사일)은 코어 없이 열려 있고, 나머지는 코어 포인트 1개로 연다.
 * angle은 마법 나무에서 가운데로부터 뻗는 방향(도, 0 = 오른쪽, 90 = 아래), color는 계열 테두리 색.
 */
public enum MagicSchool {
    BASIC("기초", 0, 0xFFE9C176),
    FIRE("화염", -90, 0xFFE8743B),
    FROST("냉기", -30, 0xFF7FC4F0),
    NATURE("자연", 30, 0xFF8BC34A),
    HOLY("신성", 90, 0xFFF3E39A),
    DARK("어둠", 150, 0xFFA77BD8),
    WIND("바람", 210, 0xFF5FD3C0);

    public final String displayName;
    public final int angle;
    public final int color;

    MagicSchool(String displayName, int angle, int color) {
        this.displayName = displayName;
        this.angle = angle;
        this.color = color;
    }

    public boolean needsCore() {
        return this != BASIC;
    }
}
