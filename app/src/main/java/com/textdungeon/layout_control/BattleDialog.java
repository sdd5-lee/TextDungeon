package com.textdungeon.layout_control;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.textdungeon.R; // R 패키지 경로는 프로젝트에 맞게 수정
import com.textdungeon.buttons.BattleButton;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.model.Item;
import com.textdungeon.model.LearnedMagic;
import com.textdungeon.model.Magic;
import com.textdungeon.model.Monster;
import com.textdungeon.player.MagicScroll;
import com.textdungeon.player.Player;
import com.textdungeon.system.BattleSystem;

import java.util.Map;
import java.util.Objects;

public class BattleDialog extends Dialog {

    private BattleSystem battleSystem;
    private final Player player;
    private final Monster monster;
    private final Context context;

    // 최대 체력 (바(Bar) 비율 계산용)
    private int playerMaxHp;
    private int monsterMaxHp;

    private TextView playerHpText;
    private TextView monsterHpText;
    private TextView logView;
    private View playerHpBar, monsterHpBar;
    private ScrollView magicScrollView, itemsScrollView;

    public BattleDialog(Context context, Player player, Monster monster) {
        super(context);
        this.context = context;
        this.player = player;
        this.monster = monster;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // 사용자님이 만드신 배틀 레이아웃 연결
        setContentView(R.layout.battle_dialog);
        setCancelable(false);
        setCanceledOnTouchOutside(false);
        // 배경을 투명하게 하여 모서리 각진 부분 제거 (필요 시)
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // 최대 체력 기록
        playerMaxHp = player.getMaxHp();
        monsterMaxHp = monster.getHp(); // 몬스터의 초기 체력을 최대 체력으로 간주

        // 전투 시스템 초기화
        battleSystem = new BattleSystem(player, monster, context);

        initViews();
        setupListeners();
        updateUI();

        appendLog("야생의 [" + monster.getName() + "]이(가) 나타났다!");
    }
    @Override
    protected void onStart() {
        super.onStart();

        Window window = getWindow();
        if (window != null) {
            int width = (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.95);
            int height = (int) (getContext().getResources().getDisplayMetrics().heightPixels * 0.90);

            window.setLayout(width, height);
        }
    }
    private void initViews() {
        // UI 요소
        playerHpText = findViewById(R.id.player_hp_text);
        playerHpBar = findViewById(R.id.player_hp_bar);

        monsterHpText = findViewById(R.id.monster_hp_text);
        monsterHpBar = findViewById(R.id.monster_hp_bar);

        logView = findViewById(R.id.battle_log_view);

        magicScrollView = findViewById(R.id.magic_scroll_view);
        itemsScrollView = findViewById(R.id.items_scroll_view);

        // 이름 세팅
        TextView playerName = findViewById(R.id.player_name);
        TextView monsterName = findViewById(R.id.monster_name);

        playerName.setText(player.getName()); // 혹은 player.getName()
        monsterName.setText(battleSystem.getEnemyName());
    }

    private void setupListeners() {
        // [ATTACK] 버튼
        findViewById(R.id.battle_btn_attack).setOnClickListener(v -> {
            magicScrollView.setVisibility(View.GONE);
            itemsScrollView.setVisibility(View.GONE);
            executeAction(1, null);
        });
        // [GUARD] 버튼
        findViewById(R.id.battle_btn_guard).setOnClickListener(v -> {
            magicScrollView.setVisibility(View.GONE);
            itemsScrollView.setVisibility(View.GONE);
            executeAction(2, null);
        });
        // [ESCAPE] 버튼
        findViewById(R.id.battle_btn_escape).setOnClickListener(v -> {
            magicScrollView.setVisibility(View.GONE);
            itemsScrollView.setVisibility(View.GONE);
            executeAction(3, null);
        });

        // [MAGIC] 탭 토글
        findViewById(R.id.battle_btn_magic).setOnClickListener(v -> {
            itemsScrollView.setVisibility(View.GONE);
            if (magicScrollView.getVisibility() == View.VISIBLE) {
                magicScrollView.setVisibility(View.GONE);
            } else {
                magicScrollView.setVisibility(View.VISIBLE);
            }
        });
        // [ITEMS] 탭 토글
        findViewById(R.id.battle_btn_items).setOnClickListener(v -> {
            magicScrollView.setVisibility(View.GONE);
            if (itemsScrollView.getVisibility() == View.VISIBLE) {
                itemsScrollView.setVisibility(View.GONE);
            } else {
                itemsScrollView.setVisibility(View.VISIBLE);
            }
        });
        addMagic();
        addItem();
    }
    private void addMagic(){
        GridLayout magicContainer = findViewById(R.id.magic_container);
        magicContainer.removeAllViews();

        MagicScroll magicScroll = player.getMagicScroll();
        DataControlTower dt = DataControlTower.getInstance(getContext());

        for (LearnedMagic ma : magicScroll.getLearnedMagics()) {
            Magic magic = dt.getMagicManager().spawn(ma.getMagicId());
            if (magic == null){{
                continue;
            }}
            BattleButton button = new BattleButton(getContext(),"magic_test",magic.getName(),magic.getCount(),"시전하기");
            button.setOnClickListener(v -> {
                executeAction(4, ma.getMagicId());
            });
            magicContainer.addView(button);
        }
    }
    private void addItem() {
        GridLayout itemContainer = findViewById(R.id.item_container);
        itemContainer.removeAllViews();

        DataControlTower dt = DataControlTower.getInstance(getContext());
        Map<String, Integer> inventory = player.getInventory().getItemMap();

        for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
            String itemId = entry.getKey();
            int count = entry.getValue();

            if (count <= 0) {
                continue;
            }

            Item itemData = dt.getItemManager().spawn(itemId);
            if (itemData == null || !Objects.equals(itemData.getType(), "consumables")) {
                continue;
            }

            // 2. 버튼 생성
            BattleButton button = new BattleButton(getContext(), "item_test", itemData.getName(), count, "사용하기");

            button.setOnClickListener(v -> {
                if (player.getInventory().consumeItem(itemId) && itemData.itemUse(player)) {

                    appendLog("\n[" + itemData.getName() + "]을(를) 사용했습니다!");
                    updateUI(); // 체력바 갱신 (효과가 적용되었으므로)
                    addItem();  // 수량이 깎였으니 가방 목록 새로고침
                } else {
                    appendLog("아이템이 부족하거나 사용하지 못하는 아이템입니다.");
                }
            });

            itemContainer.addView(button);
        }
    }

    // 전투 액션 실행 및 화면 갱신
    private void executeAction(int choice, String magicId) {
        if (battleSystem.isBattleOver()) {
            dismiss(); // 이미 끝났으면 창 닫기
            return;
        }

        // BattleSystem에 액션 전달 후 로그 받기
        String resultLog = battleSystem.playerAction(choice, magicId);
        appendLog(resultLog);
        updateUI();

        if (battleSystem.isBattleOver()) {
            appendLog("\n[전투가 종료되었습니다. 2초 뒤 돌아갑니다...]");

            findViewById(R.id.actions_area).setVisibility(View.GONE);

            new Handler(Looper.getMainLooper()).postDelayed(this::dismiss, 2000);
        }
    }

    private void updateUI() {
        int currentHp = Math.max(0, player.getStat().getHp());
        int enemyHp = Math.max(0, battleSystem.getEnemyHp());

        // 텍스트 업데이트
        playerHpText.setText("HP: " + currentHp + " / " + playerMaxHp);
        monsterHpText.setText(enemyHp + " / " + monsterMaxHp);

        // 플레이어 HP Bar 너비 조절 (Weight 활용)
        LinearLayout.LayoutParams playerParams = (LinearLayout.LayoutParams) playerHpBar.getLayoutParams();
        playerParams.weight = (float) currentHp / playerMaxHp;
        playerHpBar.setLayoutParams(playerParams);

        // 몬스터 HP Bar 너비 조절
        LinearLayout.LayoutParams monsterParams = (LinearLayout.LayoutParams) monsterHpBar.getLayoutParams();
        monsterParams.weight = (float) enemyHp / monsterMaxHp;
        monsterHpBar.setLayoutParams(monsterParams);
    }

    // 로그 텍스트 누적
    private void appendLog(String msg) {
        String currentText = logView.getText().toString();
        logView.setText(currentText + "\n" + msg);

        // 스크롤 맨 아래로 이동
        View parent = (View) logView.getParent();
        if (parent instanceof ScrollView) {
            parent.post(() -> ((ScrollView) parent).fullScroll(View.FOCUS_DOWN));
        }
    }
}