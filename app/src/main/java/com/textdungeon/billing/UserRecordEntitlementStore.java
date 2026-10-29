package com.textdungeon.billing;

import android.content.Context;

import com.textdungeon.system.GameSave;
import com.textdungeon.system.UserRecord;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/** 보유 상품을 UserRecord(user_record.json)에 캐시로 저장한다. 오프라인일 때 이 캐시를 쓴다 */
public final class UserRecordEntitlementStore implements EntitlementStore {

    private final Context appContext;
    private final Supplier<UserRecord> record;

    /** record는 매번 최신 UserRecord를 읽도록 Supplier로 받는다 (DataControlTower가 기록을 바꿔 끼울 수 있다) */
    public UserRecordEntitlementStore(Context appContext, Supplier<UserRecord> record) {
        this.appContext = appContext;
        this.record = record;
    }

    @Override
    public Set<String> loadOwned() {
        return new HashSet<>(record.get().getOwnedProducts());
    }

    @Override
    public boolean saveOwned(Set<String> ownedProductIds) {
        UserRecord r = record.get();
        Set<String> previous = new HashSet<>(r.getOwnedProducts());
        r.setOwnedProducts(ownedProductIds);
        if (GameSave.saveUserRecord(appContext, r)) return true;
        // 파일에 못 썼으면 메모리도 되돌려서, 확인(acknowledge)하지 않은 상품이 이번 실행에서 쓰이지 않게 한다
        r.setOwnedProducts(previous);
        return false;
    }
}
