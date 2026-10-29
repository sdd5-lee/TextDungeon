package com.textdungeon.billing;

import java.util.Set;

/** 보유 상품 목록을 읽고 쓰는 곳. 1단계는 UserRecord, 2단계는 서버(Firestore) 구현을 더한다 */
public interface EntitlementStore {
    Set<String> loadOwned();

    /** 원자적 저장까지 끝나면 true. false면 호출한 쪽은 acknowledge 하지 않는다 */
    boolean saveOwned(Set<String> ownedProductIds);
}
