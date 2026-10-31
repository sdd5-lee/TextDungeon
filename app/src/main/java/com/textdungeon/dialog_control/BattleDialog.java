package com.textdungeon.dialog_control;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.textdungeon.R; // R 패키지 경로는 프로젝트에 맞게 수정
import com.textdungeon.activity_control.GameImages;
import com.textdungeon.buttons.BattleButton;
import com.textdungeon.buttons.MagicCardButton;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.model.Item;
import com.textdungeon.model.LearnedMagic;
import com.textdungeon.model.Magic;
import com.textdungeon.model.Monster;
import com.textdungeon.model.Trait;
import com.textdungeon.player.MagicScroll;
import com.textdungeon.player.Player;
import com.textdungeon.system.BattleSystem;

import java.util.Map;
import java.util.Objects;

public class BattleDialog extends Dialog {

    /** 내 행동 뒤 적이 공격하기까지, 적이 공격한 뒤 내 차례가 돌아오기까지 기다리는 시간 */
    private static final long ENEMY_DELAY_MS = 900;
    private static final long MY_TURN_DELAY_MS = 500;
    /** 적의 턴에 잠긴 버튼 위에 덮는 어두운 푸른 회색 */
    private static final int LOCKED_TINT = 0x991B2333;
    private static final int[] ACTION_BUTTONS = {R.id.battle_btn_attack, R.id.battle_btn_guard,
            R.id.battle_btn_escape, R.id.battle_btn_magic, R.id.battle_btn_items};

    private final Handler handler = new Handler(Looper.getMainLooper());
    /** 적의 턴이라 버튼이 잠겨 있는지 */
    private boolean locked;

    private BattleSystem battleSystem;
    private final Player player;
    private final Monster monster;
    private final Context context;
    private int playerMaxHp;
    private int monsterMaxHp;

    private TextView playerHpText;
    private TextView monsterHpText;
    private TextView logView;
    private View playerHpBar, monsterHpBar;
    private ScrollView magicScrollView, itemsScrollView;
    private final int statMultiplier; // 던전의 몬스터 세기 배수
    private Runnable onUpdateCallback;

    public BattleDialog(Context context, Player player, Monster monster, int statMultiplier, Runnable onUpdateCallback) {
        super(context);
        this.context = context;
        this.player = player;
        this.monster = monster;
        this.statMultiplier = statMultiplier;
        this.onUpdateCallback = onUpdateCallback;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.dialog_battle);
        setCancelable(false);
        setCanceledOnTouchOutside(false);

        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // 최대 체력 기록
        playerMaxHp = player.getMaxHp();
        monsterMaxHp = monster.getMaxHp() * statMultiplier;

        // 전투 시스템 초기화
        battleSystem = new BattleSystem(player, monster, context, statMultiplier);

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

        Trait t = player.getTrait();
        playerName.setText((t != null ? t.displayName + " " : "") + player.getName());
        monsterName.setText(battleSystem.getEnemyName());

        // 몬스터 그림 (없으면 빈 칸 그대로)
        GameImages.set(findViewById(R.id.battle_monster_image), monster.getImgId(), R.drawable.mon_god); // 그림이 아직 없으면 이벤트 화면과 같은 그림
    }

    private void setupListeners() {
        // [ATTACK] 버튼
        findViewById(R.id.battle_btn_attack).setOnClickListener(v -> {
            showList(null);
            executeAction(1, null);
        });
        // [GUARD] 버튼
        findViewById(R.id.battle_btn_guard).setOnClickListener(v -> {
            showList(null);
            executeAction(2, null);
        });
        // [ESCAPE] 버튼
        findViewById(R.id.battle_btn_escape).setOnClickListener(v -> {
            showList(null);
            executeAction(3, null);
        });

        // [MAGIC] 탭 토글
        findViewById(R.id.battle_btn_magic).setOnClickListener(v ->
                showList(magicScrollView.getVisibility() == View.VISIBLE ? null : magicScrollView));
        // [ITEMS] 탭 토글
        findViewById(R.id.battle_btn_items).setOnClickListener(v ->
                showList(itemsScrollView.getVisibility() == View.VISIBLE ? null : itemsScrollView));
        addMagic();
        addItem();
    }

    // 마법·가방 목록 중 하나를 연다 (null이면 둘 다 닫는다). 열린 쪽 버튼은 밝게 표시
    private void showList(ScrollView list) {
        magicScrollView.setVisibility(list == magicScrollView ? View.VISIBLE : View.GONE);
        itemsScrollView.setVisibility(list == itemsScrollView ? View.VISIBLE : View.GONE);
        findViewById(R.id.battle_btn_magic).setSelected(list == magicScrollView);
        findViewById(R.id.battle_btn_items).setSelected(list == itemsScrollView);
    }
    private void addMagic(){
        GridLayout magicContainer = findViewById(R.id.magic_container);
        magicContainer.removeAllViews();

        MagicScroll magicScroll = player.getMagicScroll();
        DataControlTower dt = DataControlTower.getInstance(getContext());

        for (LearnedMagic lm : magicScroll.getLearnedMagics()) {
            Magic magic = dt.getMagicManager().spawn(lm.getMagicId());
            if (magic == null){{
                continue;
            }}
            MagicCardButton card = new MagicCardButton(getContext(), magic,
                    battleSystem.getCooldownLeft(lm.getMagicId()));
            card.setOnClickListener(v -> executeAction(4, lm.getMagicId()));
            // 두 칸에 같은 너비로 나눠 채운다
            int margin = Math.round(4 * context.getResources().getDisplayMetrics().density);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f));
            params.width = 0;
            params.height = Math.round(84 * context.getResources().getDisplayMetrics().density);
            params.setMargins(margin, margin, margin, margin);
            magicContainer.addView(card, params);
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

            BattleButton button = new BattleButton(getContext(), "item_test_img", itemData.getName(), count, "사용하기");

            button.setOnClickListener(v -> {
                if (locked) return;
                if (player.getInventory().consumeItem(itemId) && itemData.itemUse(player)) {
                    appendLog("\n[" + itemData.getName() + "]을(를) 사용했습니다!");
                    updateUI();
                    addItem();
                } else {
                    appendLog("아이템이 부족하거나 사용하지 못하는 아이템입니다.");
                }
            });

            itemContainer.addView(button);
        }
    }

    // 전투 액션 실행 및 화면 갱신. 적은 바로 공격하지 않고 잠깐 뒤에 공격한다 (그동안 버튼은 잠긴다)
    private void executeAction(int choice, String magicId) {
        if (locked) return;
        String resultLog = battleSystem.playerAction(choice, magicId);
        appendLog(resultLog);
        updateUI();
        addMagic(); // 방금 쓴 마법의 쿨타임을 카드에 보인다
        if (battleSystem.isEscapeState()){
            appendLog("\n[도망에 성공하였습니다. 2초 뒤 돌아갑니다...]");
            findViewById(R.id.actions_area).setVisibility(View.GONE);
            onUpdateCallback.run();
            handler.postDelayed(this::dismiss, 2000);
        }
        else if (battleSystem.isBattleOver()) {
            endBattle();
        }
        else if (battleSystem.isEnemyTurn()) {
            setActionsLocked(true);
            handler.postDelayed(this::enemyTurn, ENEMY_DELAY_MS);
        }
    }

    private void enemyTurn() {
        if (!isShowing()) return;
        appendLog(battleSystem.enemyAction());
        updateUI();
        if (battleSystem.getLastDamageTaken() > 0) shake(battleSystem.getLastDamageTaken());
        if (battleSystem.isBattleOver()) {
            endBattle();
            return;
        }
        handler.postDelayed(() -> setActionsLocked(false), MY_TURN_DELAY_MS);
    }

    private void endBattle() {
        appendLog("\n[전투가 종료되었습니다. 2초 뒤 돌아갑니다...]");

        findViewById(R.id.actions_area).setVisibility(View.GONE);
        handler.postDelayed(this::dismiss, 2000);
    }

    /** 적의 턴에는 아래 버튼과 마법·가방 칸을 누를 수 없게 하고 어둡게 덮는다 */
    private void setActionsLocked(boolean lock) {
        locked = lock;
        float radius = 6 * context.getResources().getDisplayMetrics().density;
        for (int id : ACTION_BUTTONS) {
            View button = findViewById(id);
            button.setEnabled(!lock);
            GradientDrawable tint = null;
            if (lock) {
                tint = new GradientDrawable();
                tint.setCornerRadius(radius);
                tint.setColor(LOCKED_TINT);
            }
            button.setForeground(tint);
        }
        // 마법 카드는 테두리를 전경에 그려서 덮지 않고 흐리게만 한다
        for (int id : new int[]{R.id.magic_container, R.id.item_container}) {
            ViewGroup list = findViewById(id);
            for (int i = 0; i < list.getChildCount(); i++) {
                View child = list.getChildAt(i);
                child.setEnabled(!lock);
                child.setAlpha(lock ? 0.45f : 1f);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacksAndMessages(null);
    }

    // 공격받으면 창을 좌우로 흔든다 (옵션에서 끌 수 있다)
    // 최대 체력의 1/3 이상을 한 번에 잃으면 가장 세게: 폭 4dp → 24dp, 시간 0.3초 → 0.5초
    private void shake(int damage) {
        if (!context.getSharedPreferences("GameSettings", Context.MODE_PRIVATE).getBoolean("screenShake", true)) return;
        float power = Math.min(1f, damage * 3f / Math.max(1, playerMaxHp));
        float d = (4 + 20 * power) * context.getResources().getDisplayMetrics().density;
        View root = findViewById(R.id.battle_root);
        ObjectAnimator.ofFloat(root, View.TRANSLATION_X, 0, -d, d, -d * 0.7f, d * 0.7f, -d * 0.3f, d * 0.3f, 0)
                .setDuration(300 + (long) (200 * power))
                .start();
    }

    private void updateUI() {
        int currentHp = Math.max(0, player.getStat().getHp());
        int enemyHp = Math.max(0, battleSystem.getEnemyHp());

        // 텍스트 업데이트
        playerHpText.setText("HP: " + currentHp + " / " + playerMaxHp);
        monsterHpText.setText(enemyHp + " / " + monsterMaxHp);

        LinearLayout.LayoutParams playerParams = (LinearLayout.LayoutParams) playerHpBar.getLayoutParams();
        playerParams.weight = (float) currentHp / playerMaxHp;
        playerHpBar.setLayoutParams(playerParams);

        // 몬스터 HP Bar 너비 조절
        LinearLayout.LayoutParams monsterParams = (LinearLayout.LayoutParams) monsterHpBar.getLayoutParams();
        monsterParams.weight = (float) enemyHp / monsterMaxHp;
        monsterHpBar.setLayoutParams(monsterParams);
    }

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