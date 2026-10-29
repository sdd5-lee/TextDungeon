package com.textdungeon.activity_control;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.textdungeon.ads.AdManager;
import com.textdungeon.ads.RewardType;
import com.textdungeon.buttons.SingleClickListener;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.system.GameSave;
import com.textdungeon.system.GemFormula;

import java.util.function.IntConsumer;

/**
 * 정산 화면(사망·클리어) 공통: "광고 보고 젬 ×2" 버튼. 정산당 1번.
 * 판 정보는 정산 때 지워지므로(resetRun), 정산에 쓴 값과 사용 여부를 화면 상태(Bundle)로 들고 다닌다.
 */
final class SettlementAdBonus {
    private static final String KEY_BASE = "settle_base_with_bonus";
    private static final String KEY_BOOST = "settle_boost";
    private static final String KEY_USED = "ad_gem_bonus_used";

    private final BaseActivity activity;
    private final TextView button;
    /** 받은 추가 젬을 화면 총액에 더한다 */
    private final IntConsumer onBonus;

    private int baseWithBonus;
    private int boost;
    private boolean settled;
    private boolean used;

    SettlementAdBonus(BaseActivity activity, TextView button, IntConsumer onBonus) {
        this.activity = activity;
        this.button = button;
        this.onBonus = onBonus;
    }

    /** 정산을 처음 했을 때 */
    void settled(int baseWithBonus, int boost) {
        this.baseWithBonus = baseWithBonus;
        this.boost = boost;
        this.settled = true;
        bind();
    }

    /** 화면이 다시 만들어졌을 때 (회전 등). 두 번 받지 않도록 사용 여부도 되살린다 */
    void restore(Bundle state) {
        settled = state.containsKey(KEY_BASE);
        baseWithBonus = state.getInt(KEY_BASE, 0);
        boost = state.getInt(KEY_BOOST, 1);
        used = state.getBoolean(KEY_USED, false);
        bind();
    }

    void save(Bundle out) {
        if (!settled) return;
        out.putInt(KEY_BASE, baseWithBonus);
        out.putInt(KEY_BOOST, boost);
        out.putBoolean(KEY_USED, used);
    }

    private void bind() {
        int bonus = GemFormula.adBonusGems(baseWithBonus, boost);
        // 이미 정산된 판으로 들어왔거나 줄 젬이 없으면 버튼을 숨긴다
        if (!settled || bonus <= 0) {
            button.setVisibility(View.GONE);
            return;
        }
        button.setVisibility(View.VISIBLE);
        if (used) {
            button.setText("젬 ×2 받음 (+" + bonus + ")");
            button.setEnabled(false);
            button.setAlpha(0.5f);
            return;
        }
        DataControlTower dt = DataControlTower.getInstance(activity);
        boolean skip = dt.getEntitlements().hasAdSkip();
        button.setText((skip ? "젬 ×2 받기" : "광고 보고 젬 ×2") + " (+" + bonus + ")");
        activity.setSfx(button);
        button.setOnClickListener(SingleClickListener.wrap(v -> {
            if (used) return;
            dt.getAdManager().showReward(activity, RewardType.GEM_DOUBLE, new AdManager.RewardCallback() {
                @Override
                public void onRewarded() {
                    if (used || activity.isFinishing() || activity.isDestroyed()) return;
                    used = true;
                    dt.getUserRecord().addGem(bonus);
                    GameSave.saveUserRecord(activity, dt.getUserRecord());
                    onBonus.accept(bonus);
                    bind();
                }

                @Override
                public void onUnavailable(String message) {
                    activity.showGameMessage(message); // 버튼은 그대로 둔다
                }
            });
        }));
    }
}
