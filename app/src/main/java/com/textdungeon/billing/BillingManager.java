package com.textdungeon.billing;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Play 결제(BillingClient)를 감싼다. DataControlTower가 앱 Context로 하나만 만든다.
 * 라이브러리 콜백이 어느 스레드에서 오는지 가정하지 않고, 모두 메인 스레드로 넘긴 뒤 처리한다.
 * UserRecord도 게임 코드가 메인 스레드에서 만지므로 지급과 저장도 메인 스레드에서 한다.
 */
public final class BillingManager implements PurchasesUpdatedListener {
    private static final String TAG = "BillingManager";

    static final String MSG_NO_STORE = "Play 스토어에 연결할 수 없습니다.";
    static final String MSG_UNSUPPORTED = "이 기기에서는 결제를 사용할 수 없습니다.";

    private final Context appContext;
    private final EntitlementStore store;
    private final PurchaseProcessor processor;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<BillingListener> listeners = new ArrayList<>();
    private final Map<Product, ProductDetails> details = new EnumMap<>(Product.class);
    /** 결제 대기(PENDING) 중인 상품. 저장하지 않는다 */
    private final Set<String> pendingProductIds = new HashSet<>();

    private BillingClient client;
    private boolean connecting;
    private int lastSetupCode = BillingClient.BillingResponseCode.OK;
    /** 연결되면 할 일 (상점에서 '구매 복원'을 눌렀는데 아직 연결 전인 경우 등) */
    private boolean restoreRequested;

    public BillingManager(Context appContext, EntitlementStore store) {
        this.appContext = appContext.getApplicationContext();
        this.store = store;
        this.processor = new PurchaseProcessor(store, this::acknowledge);
    }

    /** 연결을 시작한다. 연결되면 보유 목록 동기화와 상품 조회를 한다. 여러 번 불러도 된다 */
    public void start() {
        if (client == null) {
            client = BillingClient.newBuilder(appContext)
                    .setListener(this)
                    .enablePendingPurchases(PendingPurchasesParams.newBuilder()
                            .enableOneTimeProducts()
                            .build())
                    .enableAutoServiceReconnection() // 연결이 끊기면 다음 호출 때 자동으로 다시 연결 (PBL 8)
                    .build();
        }
        if (client.isReady() || connecting) return;
        connecting = true;
        client.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult result) {
                main.post(() -> onSetupFinished(result.getResponseCode()));
            }

            @Override
            public void onBillingServiceDisconnected() {
                main.post(() -> connecting = false);
            }
        });
    }

    private void onSetupFinished(int code) {
        connecting = false;
        lastSetupCode = code;
        if (code == BillingClient.BillingResponseCode.OK) {
            syncPurchases();
            queryProducts();
        } else {
            // 연결 실패: 아무것도 하지 않는다. 보유 기능은 캐시로 계속 쓴다
            Log.w(TAG, "결제 연결 실패: " + code);
            if (restoreRequested) {
                restoreRequested = false;
                message(code == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE ? MSG_UNSUPPORTED : MSG_NO_STORE);
            }
            notifyOffers();
        }
    }

    public boolean isReady() {
        return client != null && client.isReady();
    }

    /** 보유 목록 동기화 (앱 시작, 상점 onResume). 연결 전이면 연결부터 한다 */
    public void syncPurchases() {
        if (!isReady()) {
            start();
            return; // 연결되면 onSetupFinished에서 다시 부른다
        }
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                (result, purchases) -> main.post(() -> onPurchasesQueried(result.getResponseCode(), purchases)));
    }

    /** 상점의 '구매 복원' 버튼. 동기화 결과를 문구로 알려 준다 */
    public void restorePurchases() {
        restoreRequested = true;
        syncPurchases();
    }

    private void onPurchasesQueried(int code, List<Purchase> purchases) {
        boolean restore = restoreRequested;
        restoreRequested = false;
        if (code != BillingClient.BillingResponseCode.OK) {
            // 조회 실패: 캐시 유지 (절대 회수하지 않음)
            Log.w(TAG, "보유 목록 조회 실패: " + code);
            if (restore) message(MSG_NO_STORE);
            return;
        }
        List<PurchaseInfo> infos = new ArrayList<>();
        pendingProductIds.clear();
        if (purchases != null) {
            for (Purchase p : purchases) {
                PurchaseInfo info = toInfo(p);
                infos.add(info);
                if (info.state == PurchaseInfo.State.PENDING) pendingProductIds.addAll(info.productIds);
            }
        }
        PurchaseProcessor.SyncResult result = processor.syncOwned(infos);
        if (!result.saved) {
            message("저장에 실패했습니다. 앱을 다시 시작해 주세요.");
        } else if (restore) {
            message(result.added.isEmpty() ? "복원할 구매가 없습니다." : "구매를 복원했습니다.");
        }
        if (result.changed()) notifyEntitlements();
        notifyOffers();
    }

    /** 상품 정보와 현지 가격 조회 */
    public void queryProducts() {
        if (!isReady()) {
            start();
            return;
        }
        List<QueryProductDetailsParams.Product> list = new ArrayList<>();
        for (Product p : Product.values()) {
            list.add(QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(p.id)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build());
        }
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(list).build(),
                (result, detailsResult) -> {
                    List<ProductDetails> found = detailsResult != null
                            ? detailsResult.getProductDetailsList()
                            : Collections.<ProductDetails>emptyList();
                    main.post(() -> onProductDetails(result.getResponseCode(), found));
                });
    }

    private void onProductDetails(int code, List<ProductDetails> found) {
        if (code != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, "상품 조회 실패: " + code);
            notifyOffers();
            return;
        }
        details.clear();
        for (ProductDetails d : found) {
            Product product = Product.fromId(d.getProductId());
            // 할인 혜택을 만들지 않으므로 구매 옵션은 하나뿐이다. 없으면 그 상품은 표시하지 않는다
            if (product != null && firstOffer(d) != null) details.put(product, d);
        }
        notifyOffers();
    }

    private static ProductDetails.OneTimePurchaseOfferDetails firstOffer(ProductDetails d) {
        List<ProductDetails.OneTimePurchaseOfferDetails> offers = d.getOneTimePurchaseOfferDetailsList();
        return offers == null || offers.isEmpty() ? null : offers.get(0);
    }

    /** 화면용 상품 목록. Play에서 정보를 못 받은 상품은 빠진다 */
    public List<ProductOffer> getOffers() {
        Entitlements e = new Entitlements(store.loadOwned());
        List<ProductOffer> offers = new ArrayList<>();
        for (Product p : Product.values()) {
            ProductDetails d = details.get(p);
            if (d == null) continue;
            ProductDetails.OneTimePurchaseOfferDetails offer = firstOffer(d);
            offers.add(new ProductOffer(p, d.getName(), offer != null ? offer.getFormattedPrice() : "",
                    e.owns(p), pendingProductIds.contains(p.id), e.canBuy(p)));
        }
        return offers;
    }

    /** 상품을 못 보여줄 때 안내 문구. 보여줄 상품이 있으면 null */
    public String unavailableMessage() {
        if (lastSetupCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) return MSG_UNSUPPORTED;
        if (!isReady()) return MSG_NO_STORE;
        if (details.isEmpty()) return "판매 중인 상품을 불러오지 못했습니다.";
        return null;
    }

    /** 결과는 리스너로 온다 */
    public void launchPurchase(Activity activity, Product product) {
        ProductDetails d = details.get(product);
        ProductDetails.OneTimePurchaseOfferDetails offer = d != null ? firstOffer(d) : null;
        if (!isReady() || offer == null) {
            message(MSG_NO_STORE);
            return;
        }
        BillingFlowParams.ProductDetailsParams pdp = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(d)
                .setOfferToken(offer.getOfferToken())
                .build();
        BillingResult result = client.launchBillingFlow(activity, BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(pdp))
                .build());
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            handleFailure(result.getResponseCode());
        }
    }

    @Override
    public void onPurchasesUpdated(BillingResult result, List<Purchase> purchases) {
        int code = result.getResponseCode();
        main.post(() -> {
            if (code == BillingClient.BillingResponseCode.OK) {
                if (purchases != null) {
                    for (Purchase p : purchases) handlePurchase(toInfo(p));
                }
            } else {
                handleFailure(code);
            }
            notifyOffers();
        });
    }

    private void handlePurchase(PurchaseInfo info) {
        PurchaseProcessor.Outcome outcome = processor.process(info);
        switch (outcome) {
            case PENDING:
                pendingProductIds.addAll(info.productIds);
                message("결제가 완료되면 자동으로 지급됩니다.");
                break;
            case GRANTED:
                pendingProductIds.removeAll(info.productIds);
                message("구매 완료: " + namesOf(info.productIds));
                notifyEntitlements();
                break;
            case ALREADY_OWNED:
                pendingProductIds.removeAll(info.productIds);
                break;
            case SAVE_FAILED:
                message("저장에 실패했습니다. 앱을 다시 시작해 주세요.");
                break;
            default:
                Log.w(TAG, "처리하지 않은 구매: " + outcome + " " + info.productIds);
        }
    }

    private void handleFailure(int code) {
        switch (code) {
            case BillingClient.BillingResponseCode.USER_CANCELED:
                message("결제를 취소했습니다.");
                break;
            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                restorePurchases(); // "구매를 복원했습니다" 문구는 동기화 결과로 나온다
                break;
            case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
            case BillingClient.BillingResponseCode.NETWORK_ERROR:
            case BillingClient.BillingResponseCode.SERVICE_DISCONNECTED:
                message(MSG_NO_STORE);
                break;
            case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:
                message(MSG_UNSUPPORTED);
                break;
            default:
                Log.w(TAG, "결제 실패: " + code);
                message("결제에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    private void acknowledge(String token, BillingGateway.AckCallback cb) {
        if (!isReady()) {
            cb.onResult(false); // 다음 동기화 때 isAcknowledged가 false로 다시 와서 재시도된다
            return;
        }
        client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(token).build(),
                result -> main.post(() -> cb.onResult(
                        result.getResponseCode() == BillingClient.BillingResponseCode.OK)));
    }

    private static PurchaseInfo toInfo(Purchase p) {
        PurchaseInfo.State state;
        switch (p.getPurchaseState()) {
            case Purchase.PurchaseState.PURCHASED: state = PurchaseInfo.State.PURCHASED; break;
            case Purchase.PurchaseState.PENDING: state = PurchaseInfo.State.PENDING; break;
            default: state = PurchaseInfo.State.UNSPECIFIED;
        }
        return new PurchaseInfo(p.getProducts(), p.getPurchaseToken(), state, p.isAcknowledged());
    }

    private String namesOf(List<String> ids) {
        StringBuilder sb = new StringBuilder();
        for (String id : ids) {
            Product p = Product.fromId(id);
            if (p == null) continue;
            if (sb.length() > 0) sb.append(", ");
            ProductDetails d = details.get(p);
            sb.append(d != null && d.getName() != null && !d.getName().isEmpty() ? d.getName() : p.displayName);
        }
        return sb.toString();
    }

    // ───────────── 리스너 (메인 스레드) ─────────────

    public void addListener(BillingListener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    public void removeListener(BillingListener listener) {
        listeners.remove(listener);
    }

    private void notifyOffers() {
        List<ProductOffer> offers = getOffers();
        for (BillingListener l : new ArrayList<>(listeners)) l.onOffersChanged(offers);
    }

    private void notifyEntitlements() {
        Entitlements e = new Entitlements(store.loadOwned());
        for (BillingListener l : new ArrayList<>(listeners)) l.onEntitlementsChanged(e);
    }

    private void message(String text) {
        for (BillingListener l : new ArrayList<>(listeners)) l.onPurchaseMessage(text);
    }
}
