package com.textdungeon.billing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Play의 Purchase에서 필요한 값만 옮긴 것. Purchase는 단위 테스트에서 만들기 어렵다.
 * 변환은 BillingManager가 한다.
 */
public final class PurchaseInfo {

    public enum State { PURCHASED, PENDING, UNSPECIFIED }

    public final List<String> productIds;
    public final String purchaseToken;
    public final State state;
    public final boolean acknowledged;

    public PurchaseInfo(List<String> productIds, String purchaseToken, State state, boolean acknowledged) {
        this.productIds = productIds == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(productIds));
        this.purchaseToken = purchaseToken;
        this.state = state == null ? State.UNSPECIFIED : state;
        this.acknowledged = acknowledged;
    }
}
