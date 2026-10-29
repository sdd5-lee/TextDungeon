package com.textdungeon.billing;

/** acknowledge 같은 Play 호출을 감싼 것. 단위 테스트에서는 가짜로 바꾼다 */
public interface BillingGateway {
    void acknowledge(String purchaseToken, AckCallback callback);

    interface AckCallback {
        void onResult(boolean success);
    }
}
