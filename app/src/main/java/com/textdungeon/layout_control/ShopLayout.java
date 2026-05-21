package com.textdungeon.layout_control;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.viewpager2.widget.ViewPager2;

import com.example.textdungeon.R;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.model.Job;
import com.textdungeon.model.ShopUpgrade;
import com.textdungeon.system.GameSave;
import com.textdungeon.system.ShopSystem;
import com.textdungeon.system.UserRecord;

import java.util.ArrayList;
import java.util.List;

public class ShopLayout extends BaseActivity {
    private ShopSystem shopSystem;
    private DataControlTower dt;
    private TextView playerGem;
    private UserRecord record;

    private ViewPager2 unlockViewPager;
    private View upgradeScroll;
    private LinearLayout upgradeContainer;
    private ShopJobAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shop_main_layout);

        dt = DataControlTower.getInstance(this);
        shopSystem = new ShopSystem(dt);
        record = dt.getUserRecord();
        playerGem = findViewById(R.id.player_gem);

        TextView unlockTab = findViewById(R.id.tab_unlock);
        TextView upgradeTab = findViewById(R.id.tab_upgrade);

        unlockViewPager = findViewById(R.id.unlock_viewpager);
        upgradeScroll = findViewById(R.id.upgrade_scroll);
        upgradeContainer = findViewById(R.id.upgrade_container);

        FrameLayout backToMain = findViewById(R.id.bottom_action);

        unlockTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (unlockViewPager.getVisibility() == View.INVISIBLE) {
                    unlockViewPager.setVisibility(View.VISIBLE);
                    upgradeScroll.setVisibility(View.INVISIBLE);
                    unlockTab.setTextColor(Color.parseColor("#E9C176"));
                    upgradeTab.setTextColor(Color.parseColor("#A0A0A0"));
                }
            }
        });

        upgradeTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (upgradeScroll.getVisibility() == View.INVISIBLE) {
                    upgradeScroll.setVisibility(View.VISIBLE);
                    unlockViewPager.setVisibility(View.INVISIBLE);
                    unlockTab.setTextColor(Color.parseColor("#A0A0A0"));
                    upgradeTab.setTextColor(Color.parseColor("#E9C176"));
                }
            }
        });

        backToMain.setOnClickListener(this::moveMain);

        updateGemUI();
        unlockTabList();
        upgradeTabList();
    }

    public void moveMain(View v) {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    public void updateGemUI(){
        if (playerGem != null) {
            playerGem.setText("보유 재화: " + record.getGem() + " Gem");
        }
        GameSave.saveUserRecord(this,record);
    }

    public void unlockTabList(){
        List<Job> lockedJobs = new ArrayList<>();
        for (Job job : Job.values()) {
            if (!job.defaultUnlocked){
                lockedJobs.add(job);
            }
        }

        adapter = new ShopJobAdapter(this, lockedJobs, shopSystem, record, new Runnable() {
            @Override
            public void run() {
                updateGemUI();

                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }
        });

        unlockViewPager.setAdapter(adapter);
    }

    public void upgradeTabList(){
        LayoutInflater inflater = LayoutInflater.from(this);
        upgradeContainer.removeAllViews(); // 초기화

        for (ShopUpgrade upgrade : ShopUpgrade.values()) {
            View rowView = inflater.inflate(R.layout.shop_upgrade_card, upgradeContainer, false);

            TextView tvName = rowView.findViewById(R.id.upgrade_name);
            TextView tvLevel = rowView.findViewById(R.id.upgrade_level);
            TextView tvDesc = rowView.findViewById(R.id.upgrade_desc);
            TextView tvValue = rowView.findViewById(R.id.upgrade_value);
            TextView tvPrice = rowView.findViewById(R.id.upgrade_price);
            LinearLayout btnUpgrade = rowView.findViewById(R.id.btn_upgrade);

            // 현재 레벨 가져오기
            int currentLevel = record.getUpgradeLevel(upgrade.name());

            // 1. 기본 텍스트 세팅
            tvName.setText(upgrade.title);
            tvLevel.setText("LV. " + currentLevel + " / " + upgrade.maxLevel);
            tvDesc.setText(getUpgradeDescription(upgrade.category));

            // 2. 증가 수치 계산
            int currentValue = currentLevel * upgrade.valuePerLevel;
            int nextValue = (currentLevel + 1) * upgrade.valuePerLevel;

            // 3. 만렙 도달 여부에 따른 분기 처리
            if (currentLevel >= upgrade.maxLevel) {
                tvValue.setText("최대 효과 적용 중 (+ " + currentValue + ")");
                tvPrice.setText("MAX");

                btnUpgrade.setEnabled(false);
                btnUpgrade.setAlpha(0.5f);
            } else {
                tvValue.setText("현재: +" + currentValue + "  ▶  다음: +" + nextValue);
                tvPrice.setText(upgrade.getNextPrice(currentLevel) + "G");

                // 구매 버튼 클릭 이벤트
                btnUpgrade.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String resultMsg = shopSystem.buyUpgrade(upgrade.name());
                        Toast.makeText(ShopLayout.this, resultMsg, Toast.LENGTH_SHORT).show();

                        // 구매 성공 시 재화 UI와 리스트 갱신
                        if (resultMsg.contains("성공")) {
                            updateGemUI();
                            upgradeTabList();
                        }
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