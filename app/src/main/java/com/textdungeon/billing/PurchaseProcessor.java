package com.textdungeon.billing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 구매 상태 판단, 지급, 회수, 확인(acknowledge) 순서. BillingClient와 분리해서 단위 테스트한다.
 *
 * 원칙
 *  - 저장에 성공해야만 acknowledge 한다 (3일 안에 확인하지 않으면 Play가 자동 환불하므로,
 *    저장이 실패하면 확인하지 않고 다음 동기화에서 다시 지급·확인한다)
 *  - 회수는 Play 보유 목록 조회가 성공했을 때(syncOwned)만 한다
 *  - 비소모성 지급은 Set에 넣는 것이라 같은 구매를 여러 번 처리해도 결과가 같다
 */
public final class PurchaseProcessor {

    public enum Outcome { GRANTED, ALREADY_OWNED, PENDING, UNKNOWN_PRODUCT, SAVE_FAILED, IGNORED }

    public static final class SyncResult {
        /** 새로 생긴 상품 */
        public final Set<String> added;
        /** 환불·결제 취소로 사라진 상품 */
        public final Set<String> revoked;
        /** false면 저장에 실패해서 캐시가 그대로다 */
        public final boolean saved;

        SyncResult(Set<String> added, Set<String> revoked, boolean saved) {
            this.added = Collections.unmodifiableSet(added);
            this.revoked = Collections.unmodifiableSet(revoked);
            this.saved = saved;
        }

        public boolean changed() {
            return !added.isEmpty() || !revoked.isEmpty();
        }
    }

    private final EntitlementStore store;
    private final BillingGateway gateway;

    public PurchaseProcessor(EntitlementStore store, BillingGateway gateway) {
        this.store = store;
        this.gateway = gateway;
    }

    /** 구매 완료 콜백 한 건. 저장을 먼저 하고, 저장이 성공해야 acknowledge 한다 */
    public Outcome process(PurchaseInfo p) {
        // 결제 대기: 지급도 확인도 하지 않는다. 완료되면 콜백이나 다음 동기화로 다시 들어온다
        if (p.state == PurchaseInfo.State.PENDING) return Outcome.PENDING;
        if (p.state != PurchaseInfo.State.PURCHASED) return Outcome.IGNORED;

        Set<String> owned = new HashSet<>(store.loadOwned());
        boolean known = false;
        boolean changed = false;
        for (String id : p.productIds) {
            if (Product.fromId(id) == null) continue; // 모르는 상품은 건드리지 않는다
            known = true;
            changed |= owned.add(id); // Set이라 두 번 들어와도 한 번만 지급된다
        }
        if (!known) return Outcome.UNKNOWN_PRODUCT;

        // 확인하지 않음 → 다음 동기화에서 재시도
        if (changed && !store.saveOwned(owned)) return Outcome.SAVE_FAILED;

        if (!p.acknowledged && p.purchaseToken != null) {
            // 실패해도 보유는 유지. 다음 동기화 때 isAcknowledged가 false로 다시 와서 재시도된다
            gateway.acknowledge(p.purchaseToken, ok -> { });
        }
        return changed ? Outcome.GRANTED : Outcome.ALREADY_OWNED;
    }

    /** queryPurchasesAsync가 OK일 때만 호출한다. Play 목록으로 캐시를 덮어쓴다 */
    public SyncResult syncOwned(List<PurchaseInfo> playPurchases) {
        Set<String> fresh = new HashSet<>();
        List<String> unacknowledged = new ArrayList<>();
        if (playPurchases != null) {
            for (PurchaseInfo p : playPurchases) {
                if (p.state != PurchaseInfo.State.PURCHASED) continue; // 결제 대기는 보유로 치지 않는다
                boolean known = false;
                for (String id : p.productIds) {
                    if (Product.fromId(id) == null) continue;
                    fresh.add(id);
                    known = true;
                }
                if (known && !p.acknowledged && p.purchaseToken != null) {
                    unacknowledged.add(p.purchaseToken);
                }
            }
        }

        Set<String> old = store.loadOwned();
        Set<String> added = new HashSet<>(fresh);
        added.removeAll(old);
        Set<String> revoked = new HashSet<>(old);
        revoked.removeAll(fresh);

        if (!store.saveOwned(fresh)) {
            return new SyncResult(new HashSet<>(), new HashSet<>(), false);
        }
        for (String token : unacknowledged) {
            gateway.acknowledge(token, ok -> { });
        }
        return new SyncResult(added, revoked, true);
    }
}
