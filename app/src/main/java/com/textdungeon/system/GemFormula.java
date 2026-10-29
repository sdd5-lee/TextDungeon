package com.textdungeon.system;

/**
 * 정산 최종 젬 계산. 밸런스 조정은 이 파일의 상수만 바꾼다.
 *
 *   최종 젬 = (baseGems + 클리어 보너스) × 부스터 배율 × 광고 배율
 *   부스터 배율 = 1 + 보유한 젬 부스터 개수 (×1~×4)
 *   광고 배율 = 정산 화면에서 광고를 봤거나(광고 스킵이면 버튼만) ×2, 아니면 ×1. 정산당 1번
 *
 * 최대 ×8. 너무 빠르다고 느껴지면 첫 조정 수단은 AD_MULTIPLIER_PERCENT를 150으로 낮추는 것이다.
 */
public final class GemFormula {

    /** 광고(또는 광고 스킵)로 받는 배율, 백분율. 200 = ×2 */
    public static final int AD_MULTIPLIER_PERCENT = 200;

    private GemFormula() { }

    /** baseWithBonus = RunSettlement.baseGems + 클리어 보너스 */
    public static int finalGems(int baseWithBonus, int boostMultiplier, boolean adBonus) {
        long v = (long) Math.max(0, baseWithBonus) * Math.max(1, boostMultiplier);
        if (adBonus) v = v * AD_MULTIPLIER_PERCENT / 100;
        return (int) Math.min(Integer.MAX_VALUE, v); // int 넘침 방지
    }

    /** 정산 화면에서 광고를 본 뒤 추가로 줄 젬 */
    public static int adBonusGems(int baseWithBonus, int boostMultiplier) {
        return finalGems(baseWithBonus, boostMultiplier, true)
                - finalGems(baseWithBonus, boostMultiplier, false);
    }
}
