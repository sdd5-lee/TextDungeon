package com.textdungeon.ads;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;

import com.textdungeon.billing.Entitlements;
import com.textdungeon.billing.Product;

import org.junit.Test;

import java.util.Collections;

// ══════════════════════════════════════════════
//  AdManager 테스트: 광고 스킵을 보유하면 광고 SDK 없이 바로 보상
// ══════════════════════════════════════════════
public class AdManagerTest {

    @Test
    public void 광고_스킵이면_바로_보상하고_화면을_건드리지_않는다() {
        Context context = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(context);
        Activity activity = mock(Activity.class);
        AdManager.RewardCallback callback = mock(AdManager.RewardCallback.class);
        Entitlements skip = new Entitlements(Collections.singleton(Product.REMOVE_ADS.id));

        AdManager ads = new AdManager(context, () -> skip);
        ads.initialize(activity); // 광고 스킵이면 동의 창도 SDK도 켜지 않는다
        ads.showReward(activity, RewardType.GEM_DOUBLE, callback);

        verify(callback).onRewarded();
        verify(callback, never()).onUnavailable(anyString());
        verifyNoInteractions(activity);
    }
}
