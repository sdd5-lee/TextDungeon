package com.textdungeon.activity_control;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    private RecyclerView collectionRecycler;

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
        collectionRecycler = findViewById(R.id.collection_recycler);

        collectionRecycler.setLayoutManager(new GridLayoutManager(this, 4));

        Button btnBack = findViewById(R.id.btn_back);

        setSfx(btnBack, btnMainAchieve, btnMainCollection);

        btnMainAchieve.setOnClickListener(v -> switchMainMode(false));
        btnMainCollection.setOnClickListener(v -> switchMainMode(true));
        btnBack.setOnClickListener(v -> finish());

        switchMainMode(false);
    }

    private void switchMainMode(boolean toCollection) {
        isCollectionMode = toCollection;

        btnMainAchieve.setTextColor(Color.parseColor(!isCollectionMode ? "#E9C176" : "#A0A0A0"));
        btnMainCollection.setTextColor(Color.parseColor(isCollectionMode ? "#E9C176" : "#A0A0A0"));

        achievementList.setVisibility(!isCollectionMode ? View.VISIBLE : View.GONE);
        collectionRecycler.setVisibility(isCollectionMode ? View.VISIBLE : View.GONE);

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

                String desc = mon.getDescription() + "\n\n[기본 스탯]\n공격력: " + mon.getAttack() + " / 체력: " + mon.getMaxHp();

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
            collectionAdapter = new CollectionAdapter(this, currentCollectionList, data -> showCollectionDetail(data));
            collectionRecycler.setAdapter(collectionAdapter);
        } else {
            collectionAdapter.notifyDataSetChanged();
        }
    }

    private void showCollectionDetail(CollectionData data) {
        View cardView = getLayoutInflater().inflate(R.layout.row_collection_card, null);

        ImageView charImage = cardView.findViewById(R.id.char_image);
        TextView charName = cardView.findViewById(R.id.char_name);
        TextView charDesc = cardView.findViewById(R.id.char_desc);
        TextView unlockStatus = cardView.findViewById(R.id.unlock_status);
        LinearLayout btnUnlock = cardView.findViewById(R.id.btn_unlock);

        if (data.isDiscovered()) {
            charName.setText(data.getName());
            charName.setTextColor(Color.parseColor("#E9C176"));
            charDesc.setText(data.getDesc());

            unlockStatus.setText("닫기");
            unlockStatus.setTextColor(Color.parseColor("#E9C176"));

            int resId = getResources().getIdentifier(data.getImgId(), "drawable", getPackageName());
            if (resId != 0) {
                charImage.setImageResource(resId);
            } else {
                charImage.setImageResource(R.drawable.mon_goblin);
            }
        } else {
            charName.setText("???");
            charName.setTextColor(Color.parseColor("#A0A0A0"));
            charDesc.setText("아직 발견하지 못한 대상입니다.");

            unlockStatus.setText("닫기");
            unlockStatus.setTextColor(Color.parseColor("#A0A0A0"));
            charImage.setImageResource(android.R.drawable.ic_menu_help);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(cardView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // 하단 버튼 클릭 시 팝업 닫기
        btnUnlock.setOnClickListener(v -> {
            setSfx(btnUnlock);
            dialog.dismiss();
        });

        dialog.show();
    }
}