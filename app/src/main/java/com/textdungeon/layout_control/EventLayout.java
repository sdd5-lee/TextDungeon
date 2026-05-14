package com.textdungeon.layout_control;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.textdungeon.R;
import com.textdungeon.ai.AiCallback;
import com.textdungeon.buttons.ChoiceButton;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.event.BattleEvent;
import com.textdungeon.model.Monster;
import com.textdungeon.player.Player;
import com.textdungeon.system.EventManager;

public class EventLayout extends BaseActivity {

    // ── 의존 객체 ──────────────────────────────
    private DataControlTower dt;
    private Player player;
    private EventManager eventManager;

    // ── UI 상태 ────────────────────────────────
    private TextView eventDesc;
    private LinearLayout choiceButtons;
    private BattleEvent currentEvent;
    private boolean isDiceUsed = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_layout);

        dt = DataControlTower.getInstance(this);
        player = dt.getPlayer();
        eventManager = new EventManager(dt);

        currentEvent = eventManager.pickRandomEvent();
        renderEvent();

        setupBackButton();
    }

    private void renderEvent() {
        if (player == null) return;

        eventDesc    = findViewById(R.id.event_description);
        choiceButtons = findViewById(R.id.choice_container);

        eventDesc.setText("");
        choiceButtons.removeAllViews();

        appendDesc(currentEvent.getName());
        appendDesc(currentEvent.getDescription());

        updatePlayerHeader();

        renderEventImage();

        renderChoiceButtons();

        if (!isDiceUsed) {
            renderDiceButton();
        }
    }
    private void updatePlayerHeader() {
        ((TextView) findViewById(R.id.player_level)).setText("LV. " + player.getLevel());
        ((TextView) findViewById(R.id.hp_text)).setText(
                player.getStat().getHp() + " / " + player.getMaxHp());
        ((TextView) findViewById(R.id.xp_text)).setText(
                player.getStat().getExp() + " / " + player.getStat().getMaxExp());
        ((TextView) findViewById(R.id.stat_str)).setText(" " + player.getStat().getStrength());
        ((TextView) findViewById(R.id.stat_wis)).setText(" " + player.getStat().getWisdom());
        ((TextView) findViewById(R.id.stat_health)).setText(" " + player.getStat().getHealth());
        ((TextView) findViewById(R.id.stat_agi)).setText(" " + player.getStat().getAgility());
        ((TextView) findViewById(R.id.floor_number)).setText(
                eventManager.getCurrentFloor() + " F");
    }

    private void renderEventImage() {
        ImageView eventImage = findViewById(R.id.event_image);
        eventImage.setAlpha(1.0f);
        String imageName = currentEvent.getImgId();
        int imageResId = getResources().getIdentifier(imageName, "drawable", getPackageName());
        eventImage.setImageResource(imageResId != 0 ? imageResId : R.drawable.mon_test);
    }

    private void renderChoiceButtons() {
        int index = 0;
        for (String choiceText : currentEvent.getChoices()) {
            ChoiceButton button = new ChoiceButton(this);
            button.setTextView(choiceText);
            button.setLayoutParams(matchParentWrapContent());
            int finalIndex = index;
            button.setOnClickListener(v -> onChoiceSelected(finalIndex));
            choiceButtons.addView(button);
            index++;
        }
    }

    private void renderDiceButton() {
        ChoiceButton diceButton = new ChoiceButton(this);
        diceButton.setTextView("혼돈의 주사위 사용하기 (" + player.getDiceChane() + "개)");
        diceButton.setLayoutParams(matchParentWrapContent());
        diceButton.setOnClickListener(v -> onDiceSelected());
        choiceButtons.addView(diceButton);
    }
    private void onChoiceSelected(int index) {
        if (currentEvent == null) {
            appendDesc("시스템: 이벤트를 찾을 수 없습니다.");
            return;
        }
        choiceButtons.removeAllViews();

        String monsterId = currentEvent.getEnemyId();
        if (monsterId != null && !monsterId.isEmpty()) {
            showBattleDialog(monsterId, index);
        } else {
            applyEventResult(index);
        }
    }
    private void applyEventResult(int choiceIndex) {
        int levelSnapshot = eventManager.snapshotLevel();

        String result = eventManager.applyReward(currentEvent, choiceIndex);

        if (result == null) {
            appendDesc("인벤토리가 가득 찼습니다. 버릴 아이템을 선택해주세요.");
            // TODO: 인벤토리 다이얼로그를 열고, 버리기 완료 후 applyEventResult 재호출
            return;
        }

        appendDesc("결과 : " + result);
        updatePlayerHeader();

        if (eventManager.didLevelUp(levelSnapshot)) {
            showLevelUpDialog();
        } else {
            showNextFloorButton();
        }
    }
    private void onDiceSelected() {
        if (player.getDiceChane() <= 0) return;
        isDiceUsed = true;
        player.useDice();

        choiceButtons.removeAllViews();
        appendDesc("🎲 운명을 재구성하는 중...");

        dt.getChaosManager().requestChaosChoice(
                eventManager.getCurrentFloor(),
                player.getStat(),
                dt.getItemManager().getAll(),
                currentEvent,
                new AiCallback() {
                    @Override
                    public void onSuccess(BattleEvent updatedEvent) {
                        runOnUiThread(() -> {
                            if (updatedEvent != null && updatedEvent.getChoices().size() > 2) {
                                currentEvent = updatedEvent;
                                appendDesc("시스템: 새로운 선택지가 생성되었습니다.");
                            } else {
                                isDiceUsed = false;
                                player.addDiceChane(1);
                                appendDesc("시스템: 혼돈의 신이 응답하지 않습니다.");
                            }
                            renderEvent();
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(() -> {
                            isDiceUsed = false;
                            player.addDiceChane(1);
                            renderEvent();
                            appendDesc("시스템: 오류가 발생했습니다.");
                        });
                    }
                }
        );
    }

    private void showBattleDialog(String monsterId, int choiceIndex) {
        Monster monster = eventManager.spawnMonster(monsterId);
        if (monster == null) {
            appendDesc("시스템: 몬스터 [" + monsterId + "] 데이터를 찾을 수 없습니다.");
            return;
        }
        BattleDialog battleDialog = new BattleDialog(this, player, monster);
        battleDialog.setOnDismissListener(dialog -> {
            if (eventManager.isPlayerDead()) {
                // TODO: 게임오버 화면 연결 (2번 구현 시 여기만 수정)
                appendDesc("💀 플레이어가 사망했습니다...");
            } else {
                applyEventResult(choiceIndex);
            }
        });
        battleDialog.show();
    }

    private void showLevelUpDialog() {
        appendDesc("레벨업! " + player.getLevel() + "레벨이 되었습니다!");
        StatDialog levelUpDialog = new StatDialog(this, player);
        levelUpDialog.setOnDismissListener(dialog -> {
            renderEvent();
            showNextFloorButton();
        });
        levelUpDialog.show();
    }

    private void showNextFloorButton() {
        eventManager.goNextFloor();
        choiceButtons.removeAllViews();

        ChoiceButton button = new ChoiceButton(this);
        button.setTextView("다음층으로 (" + eventManager.getCurrentFloor() + "F)");
        button.setOnClickListener(v -> {
            isDiceUsed = false;
            currentEvent = eventManager.pickRandomEvent();
            renderEvent();
        });
        choiceButtons.addView(button);
    }
    private void setupBackButton() {
        getOnBackPressedDispatcher().addCallback(this,
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        new android.app.AlertDialog.Builder(EventLayout.this)
                                .setTitle("게임 종료")
                                .setMessage("게임을 종료하시겠습니까?\n(현재 층수: "
                                        + eventManager.getCurrentFloor() + "F)")
                                .setPositiveButton("저장 후 종료", (dialog, which) -> {
                                    dt.saveGame();
                                    finishAffinity();
                                })
                                .setNegativeButton("취소", null)
                                .show();
                    }
                });
    }
    private void appendDesc(String text) {
        if (eventDesc == null) eventDesc = findViewById(R.id.event_description);
        eventDesc.append(" " + text + "\n");
    }

    private LinearLayout.LayoutParams matchParentWrapContent() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }
}