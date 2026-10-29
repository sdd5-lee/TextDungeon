package com.textdungeon.billing;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** 보유 상품으로 계산한 권한. 만든 뒤 바뀌지 않는다. */
public final class Entitlements {

    private final Set<String> owned;

    public Entitlements(Set<String> ownedProductIds) {
        this.owned = ownedProductIds == null
                ? Collections.<String>emptySet()
                : Collections.unmodifiableSet(new HashSet<>(ownedProductIds));
    }

    public boolean owns(Product product) {
        return product != null && owned.contains(product.id);
    }

    public boolean hasAdSkip() {
        return owns(Product.REMOVE_ADS);
    }

    /** 1 + 보유한 부스터 개수. 순서와 무관하다 (II만 있어도 ×2) */
    public int gemBoostMultiplier() {
        int count = 0;
        for (Product p : Product.values()) {
            if (p.kind == Product.Kind.GEM_BOOST && owns(p)) count++;
        }
        return 1 + count;
    }

    /** 화면에서 구매 버튼을 켤지. 부스터는 앞 단계를 보유해야 한다 */
    public boolean canBuy(Product product) {
        if (product == null || owns(product)) return false;
        Product previous = product.previousTier();
        return previous == null || owns(previous);
    }

    public Set<String> ownedIds() {
        return owned;
    }
}
