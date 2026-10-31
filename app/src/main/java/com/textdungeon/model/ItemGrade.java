package com.textdungeon.model;

/**
 * 아이템 등급. 보이는 것만 다르다 (칸 테두리·이름 색). 스탯·가격은 등급과 상관없다.
 * item_list.json의 "grade"에 이름(COMMON 등)으로 적고, 없거나 모르는 값이면 COMMON.
 * 처음 나오는 층으로 정했다: 1~10 일반, 11~20 고급, 21~35 희귀, 36~50 영웅, 51~ 영웅·전설.
 * 보스가 주는 장비는 한 등급 위.
 */
public enum ItemGrade {
    COMMON("일반", 0xFFB0B0B0),
    UNCOMMON("고급", 0xFF5BC46A),
    RARE("희귀", 0xFF4A90E2),
    EPIC("영웅", 0xFFB066F0),
    LEGENDARY("전설", 0xFFF0A93B);

    public final String displayName;
    public final int color;

    ItemGrade(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
    }
}
