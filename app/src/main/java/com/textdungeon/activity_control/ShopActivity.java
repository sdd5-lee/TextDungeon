package com.textdungeon.activity_control;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.textdungeon.R;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.model.Job;
import com.textdungeon.model.ShopUpgrade;
import com.textdungeon.model.Trait;
import com.textdungeon.system.GameSave;
import com.textdungeon.system.ShopSystem;
import com.textdungeon.system.UserRecord;

import java.util.ArrayList;
import java.util.List;

public class ShopActivity extends BaseActivity {
    private ShopSystem shopSystem;
    private DataControlTower dt;
    private TextView playerGem;
    private UserRecord record;

    private TextView tabUnlock, tabTrait, tabUpgrade;
    private TextView pageIndicator; // ★ 추가됨
    private ViewPager2 unlockViewPager, unlockTraitViewPager;
    private View upgradeScroll;
    private LinearLayout upgradeContainer;

    private ShopUnlockAdapter jobAdapter;
    private ShopUnlockAdapter traitAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_shop);

        dt = DataControlTower.getInstance(this);
        shopSystem = new ShopSystem(dt);
        record = dt.getUserRecord();
        record.addGem(10000000);

        playerGem = findViewById(R.id.player_gem);

        tabUnlock = findViewById(R.id.tab_unlock);
        tabTrait = findViewById(R.id.tab_trait);
        tabUpgrade = findViewById(R.id.tab_upgrade);
        pageIndicator = findViewById(R.id.page_indicator);

        unlockViewPager = findViewById(R.id.unlock_viewpager);
        unlockTraitViewPager = findViewById(R.id.unlock_trait_viewpager);
        upgradeScroll = findViewById(R.id.upgrade_scroll);
        upgradeContainer = findViewById(R.id.upgrade_container);

        FrameLayout backToMain = findViewById(R.id.bottom_action);

        setSfx(tabUnlock, tabTrait, tabUpgrade, backToMain);

        tabUnlock.setOnClickListener(v -> switchTab(0));
        tabTrait.setOnClickListener(v -> switchTab(1));
        tabUpgrade.setOnClickListener(v -> switchTab(2));

        backToMain.setOnClickListener(this::moveMain);

        updateGemUI();
        unlockJobTabList();
        unlockTraitTabList();
        upgradeTabList();

        switchTab(0);
    }

    private void switchTab(int index) {
        unlockViewPager.setVisibility(index == 0 ? View.VISIBLE : View.INVISIBLE);
        unlockTraitViewPager.setVisibility(index == 1 ? View.VISIBLE : View.INVISIBLE);
        upgradeScroll.setVisibility(index == 2 ? View.VISIBLE : View.INVISIBLE);

        tabUnlock.setTextColor(Color.parseColor(index == 0 ? "#E9C176" : "#A0A0A0"));
        tabTrait.setTextColor(Color.parseColor(index == 1 ? "#E9C176" : "#A0A0A0"));
        tabUpgrade.setTextColor(Color.parseColor(index == 2 ? "#E9C176" : "#A0A0A0"));

        if (index == 0) {
            pageIndicator.setVisibility(View.VISIBLE);
            if (jobAdapter != null) {
                pageIndicator.setText((unlockViewPager.getCurrentItem() + 1) + " / " + jobAdapter.getItemCount());
            }
        } else if (index == 1) {
            pageIndicator.setVisibility(View.VISIBLE);
            if (traitAdapter != null) {
                pageIndicator.setText((unlockTraitViewPager.getCurrentItem() + 1) + " / " + traitAdapter.getItemCount());
            }
        } else {
            // 업그레이드 탭에서는 인디케이터 숨김
            pageIndicator.setVisibility(View.INVISIBLE);
        }
    }

    public void moveMain(View v) {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    public void updateGemUI(){
        if (playerGem != null) {
            playerGem.setText("보유 재화: " + record.getGem() + " Gem");
        }
        GameSave.saveUserRecord(this, record);
    }

    public void unlockJobTabList(){
        List<Job> lockedJobs = new ArrayList<>();
        for (Job job : Job.values()) {
            if (!job.defaultUnlocked){
                lockedJobs.add(job);
            }
        }
        jobAdapter = new ShopUnlockAdapter(this, lockedJobs, shopSystem, record, () -> {
            updateGemUI();
            if (jobAdapter != null) jobAdapter.notifyDataSetChanged();
        });
        unlockViewPager.setAdapter(jobAdapter);

        unlockViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (unlockViewPager.getVisibility() == View.VISIBLE) {
                    pageIndicator.setText((position + 1) + " / " + jobAdapter.getItemCount());
                }
            }
        });
    }

    public void unlockTraitTabList(){
        List<Trait> lockedTraits = new ArrayList<>();
        for (Trait trait : Trait.values()) {
            if (trait.price > 0) {
                lockedTraits.add(trait);
            }
        }
        traitAdapter = new ShopUnlockAdapter(this, lockedTraits, shopSystem, record, () -> {
            updateGemUI();
            if (traitAdapter != null) traitAdapter.notifyDataSetChanged();
        });
        unlockTraitViewPager.setAdapter(traitAdapter);

        unlockTraitViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (unlockTraitViewPager.getVisibility() == View.VISIBLE) {
                    pageIndicator.setText((position + 1) + " / " + traitAdapter.getItemCount());
                }
            }
        });
    }

    public void upgradeTabList(){
        LayoutInflater inflater = LayoutInflater.from(this);
        upgradeContainer.removeAllViews();

        for (ShopUpgrade upgrade : ShopUpgrade.values()) {
            View rowView = inflater.inflate(R.layout.main_shop_upgrade_card, upgradeContainer, false);

            TextView tvName = rowView.findViewById(R.id.upgrade_name);
            TextView tvLevel = rowView.findViewById(R.id.upgrade_level);
            TextView tvDesc = rowView.findViewById(R.id.upgrade_desc);
            TextView tvValue = rowView.findViewById(R.id.upgrade_value);
            TextView tvPrice = rowView.findViewById(R.id.upgrade_price);
            LinearLayout btnUpgrade = rowView.findViewById(R.id.btn_upgrade);

            setSfx(btnUpgrade);

            int currentLevel = record.getUpgradeLevel(upgrade.name());
            tvName.setText(upgrade.title);
            tvLevel.setText("LV. " + currentLevel + " / " + upgrade.maxLevel);
            tvDesc.setText(getUpgradeDescription(upgrade.category));

            int currentValue = currentLevel * upgrade.valuePerLevel;
            int nextValue = (currentLevel + 1) * upgrade.valuePerLevel;

            if (currentLevel >= upgrade.maxLevel) {
                tvValue.setText("최대 효과 적용 중 (+ " + currentValue + ")");
                tvPrice.setText("MAX");
                btnUpgrade.setEnabled(false);
                btnUpgrade.setAlpha(0.5f);
            } else {
                tvValue.setText("현재: +" + currentValue + "  ▶  다음: +" + nextValue);
                tvPrice.setText(upgrade.getNextPrice(currentLevel) + "G");

                btnUpgrade.setOnClickListener(v -> {
                    String resultMsg = shopSystem.buyUpgrade(upgrade.name());
                    showGameMessage(resultMsg);
                    if (resultMsg.contains("성공")) {
                        updateGemUI();
                        upgradeTabList();
                    }
                });
            }
            upgradeContainer.addView(rowView);
        }
    }

    private String getUpgradeDescription(String category) {
        switch (category) {
            case "STAT_POINT": return "시작 시 직접 분배할 수 있는 여유 스탯이 증가합니다.";
            case "STR": return "모든 영웅의 초기 힘 스탯이 증가합니다.";
            case "AGI": return "모든 영웅의 초기 민첩 스탯이 증가합니다.";
            case "HEALTH": return "모든 영웅의 초기 체력 스탯이 증가합니다.";
            case "WIS": return "모든 영웅의 초기 지혜 스탯이 증가합니다.";
            case "GOLD": return "던전 진입 시 소지하고 시작하는 골드가 증가합니다.";
            case "DICE_Chane": return "전투 중 혼돈의 주사위를 굴릴 수 있는 횟수가 증가합니다.";
            default: return "영웅의 능력을 영구적으로 강화합니다.";
        }
    }
}