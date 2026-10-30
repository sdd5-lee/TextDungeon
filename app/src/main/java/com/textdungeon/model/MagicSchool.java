package com.textdungeon.model;

/**
 * 마법 계열. BASIC(매직 미사일)은 코어 없이 열려 있고, 나머지는 코어 포인트 1개로 연다.
 * 세피로트의 나무에서는 BASIC 다음부터 이 순서대로 왼쪽부터 한 줄씩 자라고,
 * 둘씩 한 가지를 나눠 쓴다 (화염·냉기 / 자연·바람 / 신성·어둠). color는 계열 테두리 색.
 */
public enum MagicSchool {
    BASIC("기초", 0xFFE9C176),
    FIRE("화염", 0xFFE8743B),
    FROST("냉기", 0xFF7FC4F0),
    NATURE("자연", 0xFF8BC34A),
    WIND("바람", 0xFF5FD3C0),
    HOLY("신성", 0xFFF3E39A),
    DARK("어둠", 0xFFA77BD8);

    public final String displayName;
    public final int color;

    MagicSchool(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
    }

    public boolean needsCore() {
        return this != BASIC;
    }
}
