package com.textdungeon.billing;

import java.util.List;

/** 상점 화면이 받는 결제 소식. 모두 메인 스레드에서 불린다 */
public interface BillingListener {
    void onOffersChanged(List<ProductOffer> offers);

    void onEntitlementsChanged(Entitlements entitlements);

    /** 토스트로 보여줄 한국어 문구 */
    void onPurchaseMessage(String message);
}
