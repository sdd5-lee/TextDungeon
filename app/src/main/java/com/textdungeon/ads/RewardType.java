package com.textdungeon.ads;

/** 보상형 광고를 보는 곳. AdMob 광고 단위도 나눠서 통계를 따로 본다 */
public enum RewardType {
    /** 정산 화면(사망·클리어): 젬 ×2, 정산당 1번 */
    GEM_DOUBLE,
    /** 판 진행 중: 혼돈의 주사위 +1, 판당 1번 */
    CHAOS_DICE
}
