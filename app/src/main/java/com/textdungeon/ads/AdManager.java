package com.textdungeon.ads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.textdungeon.BuildConfig;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;
import com.textdungeon.billing.Entitlements;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * AdMob 보상형 광고. 광고 스킵을 보유하면 광고 없이 바로 보상한다.
 * 화면 코드는 보유 여부와 상관없이 showReward 하나만 부른다.
 * 생성자에서는 광고 SDK를 건드리지 않는다 (광고 스킵 분기를 단위 테스트할 수 있게).
 */
public final class AdManager {
    private static final String TAG = "AdManager";
    /** Google 공식 테스트 보상형 광고 ID. 디버그 빌드는 항상 이것만 쓴다 (실제 광고를 본인이 누르면 계정이 정지될 수 있다) */
    private static final String TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917";

    public interface RewardCallback {
        void onRewarded();

        /** 광고를 보여주지 못했을 때. 끝까지 보지 않고 닫으면 아무것도 부르지 않는다 */
        void onUnavailable(String message);
    }

    private final Context appContext;
    private final Supplier<Entitlements> entitlements;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<RewardType, RewardedAd> ready = new EnumMap<>(RewardType.class);
    private final Set<RewardType> loading = EnumSet.noneOf(RewardType.class);
    private boolean initStarted;

    /** entitlements는 매번 최신 값을 읽도록 Supplier로 받는다 */
    public AdManager(Context appContext, Supplier<Entitlements> entitlements) {
        this.appContext = appContext.getApplicationContext();
        this.entitlements = entitlements;
    }

    /**
     * 동의를 받은 뒤 MobileAds를 초기화하고 광고를 하나씩 미리 불러 둔다. 광고 스킵을 보유하면 SDK를 아예 켜지 않는다.
     * 유럽경제지역·영국·스위스에서는 UMP 동의 창을 먼저 띄운다 (창 내용은 AdMob의 '개인정보 보호 및 메시지'에서 만든다).
     * 그 밖의 나라는 동의가 필요 없다고 바로 답이 온다
     */
    public void initialize(Activity activity) {
        if (entitlements.get().hasAdSkip()) return;
        ConsentInformation consent = UserMessagingPlatform.getConsentInformation(appContext);
        consent.requestConsentInfoUpdate(activity, new ConsentRequestParameters.Builder().build(),
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, formError -> {
                    if (formError != null) Log.w(TAG, "동의 창 실패: " + formError.getMessage());
                    if (consent.canRequestAds()) ensureInitialized();
                }),
                requestError -> {
                    // 오프라인 등. 이전 실행에서 받은 동의가 있으면 그대로 쓴다
                    Log.w(TAG, "동의 정보 갱신 실패: " + requestError.getMessage());
                    if (consent.canRequestAds()) ensureInitialized();
                });
        // 이전 실행에서 이미 동의를 받았으면 갱신을 기다리지 않고 바로 켠다
        if (consent.canRequestAds()) ensureInitialized();
    }

    /** 동의가 필요한 지역이면 옵션 화면에 '광고 개인정보 설정' 버튼을 보여야 한다 */
    public boolean isPrivacyOptionsRequired() {
        return UserMessagingPlatform.getConsentInformation(appContext).getPrivacyOptionsRequirementStatus()
                == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    /** 동의를 다시 고르는 창. 고른 뒤 광고를 요청해도 되면 SDK를 켠다 */
    public void showPrivacyOptions(Activity activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity, formError -> {
            if (formError != null) Log.w(TAG, "개인정보 설정 창 실패: " + formError.getMessage());
            if (canRequestAds()) ensureInitialized();
        });
    }

    private boolean canRequestAds() {
        return UserMessagingPlatform.getConsentInformation(appContext).canRequestAds();
    }

    private void ensureInitialized() {
        if (initStarted) return;
        initStarted = true;
        // 초기화는 메인 스레드를 오래 잡을 수 있어서 백그라운드에서 한다
        new Thread(() -> MobileAds.initialize(appContext, status -> main.post(() -> {
            for (RewardType type : RewardType.values()) load(type);
        })), "AdsInit").start();
    }

    /** 광고 스킵 보유 또는 광고 준비됨 */
    public boolean canShowReward(RewardType type) {
        return entitlements.get().hasAdSkip() || ready.get(type) != null;
    }

    public void showReward(Activity activity, RewardType type, RewardCallback callback) {
        // 광고 스킵: 광고 SDK를 건드리지 않고 바로 보상
        if (entitlements.get().hasAdSkip()) {
            callback.onRewarded();
            return;
        }
        RewardedAd ad = ready.remove(type);
        if (ad == null) {
            callback.onUnavailable("광고를 준비하고 있습니다. 잠시 후 다시 시도해 주세요.");
            load(type);
            return;
        }
        AtomicBoolean earned = new AtomicBoolean(false);
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                main.post(() -> {
                    load(type); // 다음 광고 미리 불러오기
                    // 광고가 닫힌 뒤에 보상한다. 끝까지 안 보고 닫으면 earned가 false
                    if (earned.get()) callback.onRewarded();
                });
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                main.post(() -> {
                    Log.w(TAG, "광고 재생 실패: " + error.getMessage());
                    load(type);
                    callback.onUnavailable("광고를 재생하지 못했습니다.");
                });
            }
        });
        ad.show(activity, rewardItem -> earned.set(true));
    }

    private void load(RewardType type) {
        if (!initStarted) {
            // 동의를 받기 전에는 SDK를 켜지 않는다. 초기화가 끝나면 모든 종류를 불러온다
            if (canRequestAds()) ensureInitialized();
            return;
        }
        if (ready.containsKey(type) || loading.contains(type)) return;
        loading.add(type);
        RewardedAd.load(appContext, adUnitId(type), new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                main.post(() -> {
                    loading.remove(type);
                    ready.put(type, ad);
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                // 다시 시도는 다음 showReward 때 한다
                main.post(() -> {
                    loading.remove(type);
                    Log.w(TAG, type + " 광고 불러오기 실패: " + error.getMessage());
                });
            }
        });
    }

    private static String adUnitId(RewardType type) {
        if (BuildConfig.DEBUG) return TEST_REWARDED_ID;
        return type == RewardType.GEM_DOUBLE
                ? BuildConfig.ADMOB_REWARDED_SETTLE_ID
                : BuildConfig.ADMOB_REWARDED_DICE_ID;
    }
}
