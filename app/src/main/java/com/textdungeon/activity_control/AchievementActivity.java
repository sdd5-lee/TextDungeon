package com.textdungeon.activity_control;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.viewpager2.widget.ViewPager2;

import com.example.textdungeon.R;
import com.textdungeon.data.CollectionData;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.event.GameEvent;
import com.textdungeon.model.Achievement;
import com.textdungeon.model.Item;
import com.textdungeon.model.Monster;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AchievementActivity extends BaseActivity {

    private DataControlTower dt;

    private Button btnMainAchieve, btnMainCollection;
    private LinearLayout subTabContainer;
    private ListView achievementList;
    private ViewPager2 collectionPager;
    private TextView pageIndicator;

    private boolean isCollectionMode = false;

    private final String[] achieveCategories = {"전체", "도달층수", "전투"};
    private final String[] collectionCategories = {"아이템", "몬스터", "이벤트"};

    private List<Achievement> currentAchieveList = new ArrayList<>();
    private AchieveAdapter achieveAdapter;

    private List<CollectionData> currentCollectionList = new ArrayList<>();
    private CollectionAdapter collectionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievement);

        dt = DataControlTower.getInstance(this);

        btnMainAchieve = findViewById(R.id.btn_main_achieve);
        btnMainCollection = findViewById(R.id.btn_main_collection);
        subTabContainer = findViewById(R.id.sub_tab_container);
        achievementList = findViewById(R.id.achievement_list);
        collectionPager = findViewById(R.id.collection_pager);
        pageIndicator = findViewById(R.id.page_indicator);
        Button btnBack = findViewById(R.id.btn_back);

        setSfx(btnBack, btnMainAchieve, btnMainCollection);

        btnMainAchieve.setOnClickListener(v -> switchMainMode(false));
        btnMainCollection.setOnClickListener(v -> switchMainMode(true));
        btnBack.setOnClickListener(v -> finish());

        collectionPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (isCollectionMode) {
                    pageIndicator.setText((position + 1) + " / " + currentCollectionList.size());
                }
            }
        });

        switchMainMode(false);
    }

    private void switchMainMode(boolean toCollection) {
        isCollectionMode = toCollection;

        btnMainAchieve.setTextColor(Color.parseColor(!isCollectionMode ? "#E9C176" : "#A0A0A0"));
        btnMainCollection.setTextColor(Color.parseColor(isCollectionMode ? "#E9C176" : "#A0A0A0"));

        achievementList.setVisibility(!isCollectionMode ? View.VISIBLE : View.GONE);
        collectionPager.setVisibility(isCollectionMode ? View.VISIBLE : View.GONE);
        pageIndicator.setVisibility(isCollectionMode ? View.VISIBLE : View.GONE);

        buildSubTabs();
    }

    private void buildSubTabs() {
        subTabContainer.removeAllViews();
        String[] currentTabs = isCollectionMode ? collectionCategories : achieveCategories;

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(24, 0, 24, 0);

        for (String category : currentTabs) {
            Button tabBtn = new Button(this);
            tabBtn.setLayoutParams(params);
            tabBtn.setText(category);
            tabBtn.setTextColor(Color.parseColor("#E5E2E1"));
            tabBtn.setBackgroundColor(Color.TRANSPARENT);

            tabBtn.setOnClickListener(v -> {
                setSfx(tabBtn);
                updateSubTabStyles(category);
                if (isCollectionMode) {
                    loadCollectionCategory(category);
                } else {
                    loadAchieveCategory(category);
                }
            });

            subTabContainer.addView(tabBtn);
        }

        updateSubTabStyles(currentTabs[0]);
        if (isCollectionMode) {
            loadCollectionCategory(currentTabs[0]);
        } else {
            loadAchieveCategory(currentTabs[0]);
        }
    }

    private void updateSubTabStyles(String selectedCategory) {
        for (int i = 0; i < subTabContainer.getChildCount(); i++) {
            Button btn = (Button) subTabContainer.getChildAt(i);
            if (btn.getText().toString().equals(selectedCategory)) {
                btn.setTextColor(Color.parseColor("#E9C176"));
                btn.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                btn.setTextColor(Color.parseColor("#A0A0A0"));
                btn.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }
    }

    private void loadAchieveCategory(String category) {
        currentAchieveList.clear();
        List<Achievement> allAch = dt.getAchievementManager().getAllAchievements();

        for (Achievement ach : allAch) {
            if (category.equals("전체") || ach.getCategory().equals(category)) {
                currentAchieveList.add(ach);
            }
        }

        if (achieveAdapter == null) {
            achieveAdapter = new AchieveAdapter(this, currentAchieveList);
            achievementList.setAdapter(achieveAdapter);
        } else {
            achieveAdapter.notifyDataSetChanged();
        }
    }

    private void loadCollectionCategory(String category) {
        currentCollectionList.clear();

        Set<String> disItems = dt.getUserRecord().getDiscoveredItems();
        Set<String> disMonsters = dt.getUserRecord().getDiscoveredMonsters();
        Set<String> disEvents = dt.getUserRecord().getDiscoveredEvents();

        if (category.equals("아이템")) {
            for (Item item : dt.getItemManager().getAll()) {
                boolean discovered = disItems.contains(item.getId());
                currentCollectionList.add(new CollectionData(
                        item.getName(), item.getDescription(),
                        item.getImgId() != null ? item.getImgId() : "ic_weapon",
                        discovered));
            }
        } else if (category.equals("몬스터")) {
            for (Monster mon : dt.getMonsterManager().getAll()) {
                boolean discovered = disMonsters.contains(mon.getId());
                String desc = "공격력: " + mon.getAttack() + " / 체력: " + mon.getMaxHp();
                currentCollectionList.add(new CollectionData(
                        mon.getName(), desc, mon.getImgId(), discovered));
            }
        } else if (category.equals("이벤트")) {
            for (GameEvent ev : dt.getEventManager().getAll()) {
                boolean discovered = disEvents.contains(ev.getId());
                currentCollectionList.add(new CollectionData(
                        ev.getName(), ev.getDescription(), ev.getImgId(), discovered));
            }
        }

        if (collectionAdapter == null) {
            collectionAdapter = new CollectionAdapter(this, currentCollectionList);
            collectionPager.setAdapter(collectionAdapter);
        } else {
            collectionAdapter.notifyDataSetChanged();
        }

        pageIndicator.setText("1 / " + currentCollectionList.size());
        collectionPager.setCurrentItem(0, false);
    }
}