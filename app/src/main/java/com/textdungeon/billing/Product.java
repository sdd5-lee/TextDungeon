package com.textdungeon.billing;

// ══════════════════════════════════════════════
//  판매 상품 (모두 비소모성 일회성 상품)
//  id는 Play Console의 상품 ID와 반드시 같아야 한다. 한 번 만든 ID는 바꿀 수 없다
//  시나리오 DLC는 콘텐츠가 생기면 SCENARIO 종류로 한 줄 더하고 ScenarioPack에도 한 줄 더한다
//  (예: SCENARIO_FROST("scenario_frost", Kind.SCENARIO, 0))
// ══════════════════════════════════════════════
public enum Product {
    REMOVE_ADS("remove_ads", Kind.AD_SKIP, 0, "광고 스킵", "보상형 광고를 보지 않고 보상을 바로 받습니다."),
    GEM_BOOST_1("gem_boost_1", Kind.GEM_BOOST, 1, "젬 부스터 I", "정산 젬 배율 +1 (×2)"),
    GEM_BOOST_2("gem_boost_2", Kind.GEM_BOOST, 2, "젬 부스터 II", "정산 젬 배율 +1 (×3)"),
    GEM_BOOST_3("gem_boost_3", Kind.GEM_BOOST, 3, "젬 부스터 III", "정산 젬 배율 +1 (×4)");

    /** 2단계에서 소모성 상품(시나리오 주문 제작권 등)을 더할 자리를 남겨 둔다 */
    public enum Kind { AD_SKIP, GEM_BOOST, SCENARIO }

    public final String id;
    public final Kind kind;
    /** 부스터 단계 (1~3). 다른 상품은 0 */
    public final int tier;
    /** Play에서 이름을 못 받았을 때 쓰는 이름과 효과 설명 */
    public final String displayName;
    public final String effect;

    Product(String id, Kind kind, int tier, String displayName, String effect) {
        this.id = id;
        this.kind = kind;
        this.tier = tier;
        this.displayName = displayName;
        this.effect = effect;
    }

    /** 모르는 ID면 null */
    public static Product fromId(String id) {
        if (id == null) return null;
        for (Product p : values()) {
            if (p.id.equals(id)) return p;
        }
        return null;
    }

    /** 같은 종류에서 바로 앞 단계 부스터. 1단계거나 부스터가 아니면 null */
    public Product previousTier() {
        if (kind != Kind.GEM_BOOST || tier <= 1) return null;
        for (Product p : values()) {
            if (p.kind == Kind.GEM_BOOST && p.tier == tier - 1) return p;
        }
        return null;
    }
}
