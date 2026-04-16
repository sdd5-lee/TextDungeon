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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.textdungeon.R; // R 패키지 경로는 프로젝트에 맞게 수정
import com.textdungeon.model.Monster;
import com.textdungeon.player.Player;
import com.textdungeon.system.BattleSystem;

public class BattleDialog extends Dialog {

    private BattleSystem battleSystem;
    private Player player;
    private Monster monster;
    private Context context;

    // 최대 체력 (바(Bar) 비율 계산용)
    private int playerMaxHp;
    private int monsterMaxHp;

    // UI 요소
    private TextView playerName, playerHpText, monsterName, monsterHpText, logView;
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
// BattleDialog.java 내부 (onCreate 바깥에 추가)

    @Override
    protected void onStart() {
        super.onStart();

        Window window = getWindow();
        if (window != null) {
            // 스마트폰 화면의 가로 95%, 세로 90% 크기로 큼직하게 꽉 채웁니다.
            int width = (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.95);
            int height = (int) (getContext().getResources().getDisplayMetrics().heightPixels * 0.90);

        /* 만약 아예 빈틈없이 화면 전체를 덮고 싶다면 아래 코드를 쓰시면 됩니다.
        int width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
        int height = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
        */

            window.setLayout(width, height);
        }
    }
    private void initViews() {
        playerName = findViewById(R.id.player_name);
        playerHpText = findViewById(R.id.player_hp_text);
        playerHpBar = findViewById(R.id.player_hp_bar);

        monsterName = findViewById(R.id.monster_name);
        monsterHpText = findViewById(R.id.monster_hp_text);
        monsterHpBar = findViewById(R.id.monster_hp_bar);

        logView = findViewById(R.id.battle_log_view);

        magicScrollView = findViewById(R.id.magic_scroll_view);
        itemsScrollView = findViewById(R.id.items_scroll_view);

        // 이름 세팅
        playerName.setText("PLAYER"); // 혹은 player.getName()
        monsterName.setText(battleSystem.getEnemyName());
    }

    private void setupListeners() {
        // [ATTACK] 버튼
        findViewById(R.id.battle_btn_attack).setOnClickListener(v -> {
            magicScrollView.setVisibility(View.GONE);
            itemsScrollView.setVisibility(View.GONE);
            executeAction(1, null);
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

        // (임시) 마법 탭 안의 FIREBALL 레이아웃을 클릭했을 때 처리
        // 실제로는 RecyclerView나 동적 생성 코드가 들어가야 합니다.
        // XML 구조상 FIREBALL 텍스트가 있는 첫 번째 LinearLayout에 ID를 부여해야 정확하지만,
        // 테스트를 위해 직접 접근합니다.
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

        // 방금 액션으로 전투가 끝났다면?
        if (battleSystem.isBattleOver()) {
            appendLog("\n[전투가 종료되었습니다. 2초 뒤 돌아갑니다...]");

            // 모든 버튼 비활성화 (클릭 연타 방지)
            findViewById(R.id.actions_area).setVisibility(View.GONE);

            // 2초 뒤 다이얼로그 자동 종료
            new Handler(Looper.getMainLooper()).postDelayed(this::dismiss, 2000);
        }
    }

    // 체력 및 바(Bar) UI 갱신
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