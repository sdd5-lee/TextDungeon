package com.textdungeon.activity_control;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.textdungeon.R;
import com.textdungeon.ai.AiCallback;
import com.textdungeon.buttons.ChoiceButton;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.dialog_control.BattleDialog;
import com.textdungeon.dialog_control.InventoryDialog;
import com.textdungeon.dialog_control.MagicLearnDialog;
import com.textdungeon.dialog_control.PlayerInfoDialog;
import com.textdungeon.dialog_control.StatDialog;
import com.textdungeon.event.BattleEvent;
import com.textdungeon.event.GameEvent;
import com.textdungeon.model.Achievement;
import com.textdungeon.model.Monster;
import com.textdungeon.player.Player;
import com.textdungeon.system.EventManager;

import java.util.List;

public class EventActivity extends BaseActivity {

    private DataControlTower dt;
    private Player player;
    private EventManager eventManager;
    private TextView eventDesc;
    private LinearLayout choiceButtons;
    private GameEvent currentEvent;
    private boolean isDiceUsed = false;

    private Handler typingHandler = new Handler(Looper.getMainLooper());
    private Runnable typingRunnable;
    private StringBuilder typingBuffer = new StringBuilder();
    private String currentTypingText = "";
    private int currentTypingIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);

        dt = DataControlTower.getInstance(this);
        player = dt.getPlayer();
        eventManager = new EventManager(dt);

        if (eventManager.getCurrentFloor() > 50) {
            Intent intent = new Intent(this, ClearActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        GameEvent saved = dt.getDungeonControl().getCurrentEvent();
        if (saved != null) {
            currentEvent = saved;
        } else {
            currentEvent = eventManager.pickRandomEvent();
            dt.getDungeonControl().setCurrentEvent(currentEvent);
        }

        setupSkipListener();

        LinearLayout btnMagicLearn = findViewById(R.id.btn_magic_learn);
        if (btnMagicLearn != null) {
            setSfx(btnMagicLearn);
            btnMagicLearn.setOnClickListener(v -> {
                MagicLearnDialog dialog = new MagicLearnDialog(
                        EventActivity.this,
                        player,
                        dt.getMagicManager()
                );
                dialog.show();
            });
        }

        LinearLayout btnInventory = findViewById(R.id.btn_inventory);
        if (btnInventory != null) {
            setSfx(btnInventory);
            btnInventory.setOnClickListener(v -> {
                InventoryDialog dialog = new InventoryDialog(
                        EventActivity.this,
                        player,
                        dt.getItemManager(),
                        () -> runOnUiThread(this::updatePlayerHeader)
                );
                updatePlayerHeader();
                dialog.show();
            });
        }
        LinearLayout btnSystemSetting = findViewById(R.id.btn_system);
        if (btnSystemSetting != null) {
            setSfx(btnSystemSetting);
            btnSystemSetting.setOnClickListener(v -> {
                startActivityForResult(
                        new Intent(this, SystemSettingActivity.class),
                        100
                );
            });

        }
        ImageView avatar = findViewById(R.id.avatar);
        View hpArea = findViewById(R.id.hp_area);
        GridLayout statList = findViewById(R.id.stat_list);

        String jobImgName = player.getJob().img.toLowerCase();
        int resId = getResources().getIdentifier(jobImgName, "drawable", getPackageName());

        if (resId != 0) {
            avatar.setImageResource(resId);
        } else {
            avatar.setImageResource(R.drawable.dungeon_entrance);
        }

        avatar.setOnClickListener(v -> new PlayerInfoDialog(this, player).show());
        hpArea.setOnClickListener(v -> new PlayerInfoDialog(this, player).show());
        statList.setOnClickListener(v -> new PlayerInfoDialog(this, player).show());

        renderEvent();
        setupBackButton();
    }
    // ─────────────────────────────────────────────────────────────
    // UI 렌더링 & 타이핑 애니메이션
    // ─────────────────────────────────────────────────────────────
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            recreate();
        }
    }
    private void renderEvent(String extraMsg) {
        if (player == null) return;

        eventDesc = findViewById(R.id.event_description);
        choiceButtons = findViewById(R.id.choice_container);

        eventDesc.setText("");
        typingBuffer.setLength(0);
        choiceButtons.removeAllViews();

        if (extraMsg != null) {
            appendDesc(extraMsg);
        }

        appendDesc(currentEvent.getName());
        appendDesc(currentEvent.getDescription());

        updatePlayerHeader();
        renderEventImage();
        renderChoiceButtons();

        if (!isDiceUsed) {
            renderDiceButton();
        }

        startTypingAnimation();
    }

    private void renderEvent() {
        renderEvent(null);
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

        // HP 바 업데이트 추가
        View hpBar = findViewById(R.id.hp_progress);
        LinearLayout.LayoutParams hpParams = (LinearLayout.LayoutParams) hpBar.getLayoutParams();
        hpParams.weight = (float) player.getStat().getHp() / player.getMaxHp();
        hpBar.setLayoutParams(hpParams);

        // XP 바 업데이트 추가
        View xpBar = findViewById(R.id.xp_progress);
        LinearLayout.LayoutParams xpParams = (LinearLayout.LayoutParams) xpBar.getLayoutParams();
        xpParams.weight = (float) player.getStat().getExp() / player.getStat().getMaxExp();
        xpBar.setLayoutParams(xpParams);
    }

    private void renderEventImage() {
        ImageView eventImage = findViewById(R.id.event_image);
        eventImage.setAlpha(1.0f);

        String imageName = currentEvent.getImgId();
        int imageResId = 0;

        if (imageName != null && !imageName.isEmpty()) {
            imageResId = getResources().getIdentifier(
                    imageName, "drawable", getPackageName());
        }

        if (imageResId == 0 && currentEvent instanceof BattleEvent) {
            BattleEvent battleEvent = (BattleEvent) currentEvent;
            if (battleEvent.getEnemyId() != null) {
                Monster monster = eventManager.spawnMonster(battleEvent.getEnemyId());
                if (monster != null && monster.getImgId() != null) {
                    imageResId = getResources().getIdentifier(
                            monster.getImgId(), "drawable", getPackageName());
                }
            }
        }

        eventImage.setImageResource(imageResId != 0 ? imageResId : R.drawable.mon_test);
    }

    private void renderChoiceButtons() {
        int index = 0;
        for (String choiceText : currentEvent.getChoices()) {
            ChoiceButton button = new ChoiceButton(this);
            setSfx(button);

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
        setSfx(diceButton);

        diceButton.setTextView("혼돈의 주사위 사용하기 (" + player.getDiceChance() + "개)");
        diceButton.setLayoutParams(matchParentWrapContent());
        diceButton.setOnClickListener(v -> onDiceSelected());
        choiceButtons.addView(diceButton);
    }

    // ─────────────────────────────────────────────────────────────
    // 사용자 액션 처리
    // ─────────────────────────────────────────────────────────────
    private void onChoiceSelected(int index) {
        if (currentEvent == null) {
            appendDesc("시스템: 이벤트를 찾을 수 없습니다.");
            startTypingAnimation();
            return;
        }
        choiceButtons.removeAllViews();

        if (currentEvent instanceof com.textdungeon.event.ShopEvent) {
            com.textdungeon.event.ShopEvent shopEvent = (com.textdungeon.event.ShopEvent) currentEvent;
            shopEvent.openShop(this, player, dt.getItemManager());
            showNextFloorButton();
            startTypingAnimation();
            return;
        }

        if (currentEvent instanceof BattleEvent) {
            BattleEvent battleEvent = (BattleEvent) currentEvent;
            String monsterId = battleEvent.getEnemyId();

            if (monsterId != null && !monsterId.isEmpty()) {
                showBattleDialog(monsterId, index);
                return;
            }
        }
        applyEventResult(index);
    }
    private void applyEventResult(int choiceIndex) {
        int levelSnapshot = eventManager.snapshotLevel();
        String result = eventManager.applyReward(currentEvent, choiceIndex);

        if (currentEvent != null && currentEvent.getId() != null) {
            dt.getUserRecord().getDiscoveredEvents().add(currentEvent.getId());

            int itemsCount = dt.getUserRecord().getDiscoveredItems().size();
            List<Achievement> unlocked = dt.getAchievementManager().updateProgress("collection_item", itemsCount, false);
            showAchievementNotification(unlocked);
        }
        if (result.equals("full")) {
            appendDesc("인벤토리가 가득 찼습니다. 버릴 아이템을 선택해주세요.");
            startTypingAnimation();
            InventoryDialog dialog = new InventoryDialog(
                    EventActivity.this,
                    player,
                    dt.getItemManager(),
                    () -> runOnUiThread(() -> {
                        updatePlayerHeader();
                        applyEventResult(choiceIndex);
                    })
            );
            dialog.show();
            return;
        }

        appendDesc("결과 : " + result);
        updatePlayerHeader();


        if (currentEvent.isRetry(choiceIndex)) {
            renderRetryButtons();
        }
        if (eventManager.didLevelUp(levelSnapshot)) {
            showLevelUpDialog();
        } else {
            showNextFloorButton();
        }

        startTypingAnimation();
    }

    private void applyEscapeResult() {
        int levelSnapshot = eventManager.snapshotLevel();

        appendDesc("결과 : 당신은 전투를 피해 도망쳤습니다");
        updatePlayerHeader();
        if (eventManager.didLevelUp(levelSnapshot)) {
            showLevelUpDialog();
        } else {
            showNextFloorButton();
        }
        startTypingAnimation();
    }

    private void renderRetryButtons() {
        choiceButtons.removeAllViews();
        int index = 0;
        for (String choiceText : currentEvent.getChoices()) {
            if (!currentEvent.isRetry(index)) {
                ChoiceButton button = new ChoiceButton(this);
                setSfx(button);
                button.setTextView(choiceText);
                button.setLayoutParams(matchParentWrapContent());

                int finalIndex = index;
                button.setOnClickListener(v -> onChoiceSelected(finalIndex));
                choiceButtons.addView(button);
            }
            index++;
        }
    }

    private void onDiceSelected() {
        if (player.getDiceChance() <= 0) return;
        isDiceUsed = true;
        player.useDice();

        choiceButtons.removeAllViews();
        appendDesc("🎲 운명을 재구성하는 중...");
        startTypingAnimation();

        dt.getAiManager().requestChaosChoice(
                eventManager.getCurrentFloor(),
                player.getStat(),
                dt.getItemManager().getAll(),
                currentEvent,
                new AiCallback() {
                    @Override
                    public void onSuccess(GameEvent updatedEvent) {
                        runOnUiThread(() -> {
                            if (updatedEvent != null && updatedEvent.getChoices().size() > 2) {
                                currentEvent = updatedEvent;
                                dt.getDungeonControl().setCurrentEvent(currentEvent);
                                renderEvent("시스템: 새로운 선택지가 생성되었습니다.");
                            } else {
                                isDiceUsed = false;
                                player.addDiceChane(1);
                                renderEvent("시스템: 혼돈의 신이 응답하지 않습니다.");
                            }
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(() -> {
                            isDiceUsed = false;
                            player.addDiceChane(1);
                            renderEvent("시스템: 오류가 발생했습니다.");
                        });
                    }
                }
        );
    }

    // ─────────────────────────────────────────────────────────────
    // 팝업 / 다이얼로그
    // ─────────────────────────────────────────────────────────────

    private void showBattleDialog(String monsterId, int choiceIndex) {
        Monster monster = eventManager.spawnMonster(monsterId);
        if (monster == null) {
            appendDesc("시스템: 몬스터 [" + monsterId + "] 데이터를 찾을 수 없습니다.");
            startTypingAnimation();
            return;
        }
        final boolean[] escapeState = {false};
        BattleDialog battleDialog = new BattleDialog(this, player, monster, dt.getDifficulty(),
                () -> escapeState[0] = true);
        battleDialog.setOnDismissListener(dialog -> {
            if (eventManager.isPlayerDead()) {
                appendDesc("당신은 사망하였습니다.");

                ChoiceButton button = new ChoiceButton(this);
                setSfx(button);

                button.setTextView("사망 확인");
                button.setOnClickListener(v -> {
                    Intent intent = new Intent(this, DiedActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });
                choiceButtons.addView(button);
                startTypingAnimation();
            } else if(escapeState[0]) {
                applyEscapeResult();
            } else {
                dt.getUserRecord().getDiscoveredMonsters().add(monsterId);

                int monsterCount = dt.getUserRecord().getDiscoveredMonsters().size();
                List<Achievement> unlocked = dt.getAchievementManager().updateProgress("collection_monster", monsterCount, false);
                showAchievementNotification(unlocked);

                dt.getUserRecord().addKillCount();
                List<Achievement> killUnlocked = dt.getAchievementManager().updateProgress("kill", 1, true);
                showAchievementNotification(killUnlocked);

                applyEventResult(choiceIndex);
            }
        });
        battleDialog.show();
    }


    private void showLevelUpDialog() {
        appendDesc("레벨업! " + player.getLevel() + "레벨이 되었습니다!");
        StatDialog levelUpDialog = new StatDialog(this, player,
                () -> runOnUiThread(this::updatePlayerHeader));
        levelUpDialog.setOnDismissListener(dialog -> {
            showNextFloorButton();
            startTypingAnimation();
        });
        levelUpDialog.show();
    }

    // ─────────────────────────────────────────────────────────────
    // 다음 층 이동
    // ─────────────────────────────────────────────────────────────

    private void showNextFloorButton() {
        eventManager.goNextFloor();
        choiceButtons.removeAllViews();

        int currentFloor = eventManager.getCurrentFloor();
        List<Achievement> unlocked = dt.getAchievementManager().updateProgress("floor", currentFloor, false);
        showAchievementNotification(unlocked);

        if (eventManager.getCurrentFloor() > 50){
            appendDesc("던전안에 숨어있던 마왕을 쓰러트렸습니다.\n더 이상의 적은 없다.");
            ChoiceButton button = new ChoiceButton(this);

            setSfx(button);

            button.setTextView("던전탐사를 끝낸다");
            button.setOnClickListener(v -> {
                Intent intent = new Intent(this, ClearActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                dt.saveGame();
                startActivity(intent);
                finish();
            });
            choiceButtons.addView(button);
        } else {
            ChoiceButton button = new ChoiceButton(this);
            setSfx(button);

            button.setTextView("다음층으로 (" + eventManager.getCurrentFloor() + "F)");
            button.setOnClickListener(v -> {
                isDiceUsed = false;
                currentEvent = eventManager.pickRandomEvent();
                dt.getDungeonControl().setCurrentEvent(currentEvent);
                renderEvent();
            });
            choiceButtons.addView(button);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 뒤로가기 처리
    // ─────────────────────────────────────────────────────────────

    private void setupBackButton() {
        getOnBackPressedDispatcher().addCallback(this,
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        new android.app.AlertDialog.Builder(EventActivity.this)
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

    // ─────────────────────────────────────────────────────────────
    // 유틸 (타이핑 로직)
    // ─────────────────────────────────────────────────────────────
    private void setupSkipListener() {
        eventDesc = findViewById(R.id.event_description);
        eventDesc.setOnClickListener(v -> {
            if (typingRunnable != null) {
                typingHandler.removeCallbacks(typingRunnable);
                typingRunnable = null;
                if (currentTypingIndex < currentTypingText.length()) {
                    eventDesc.append(currentTypingText.substring(currentTypingIndex));
                }
                if (choiceButtons != null) {
                    choiceButtons.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void appendDesc(String text) {
        if (eventDesc == null) eventDesc = findViewById(R.id.event_description);
        typingBuffer.append(" ").append(text).append("\n");
    }

    private void startTypingAnimation() {
        if (typingRunnable != null) {
            typingHandler.removeCallbacks(typingRunnable);
        }

        currentTypingText = typingBuffer.toString();
        currentTypingIndex = 0;
        typingBuffer.setLength(0);
        if (choiceButtons != null) {
            choiceButtons.setVisibility(View.INVISIBLE);
        }

        typingRunnable = () -> {
            if (currentTypingIndex < currentTypingText.length()) {
                eventDesc.append(String.valueOf(currentTypingText.charAt(currentTypingIndex)));
                currentTypingIndex++;
                typingHandler.postDelayed(typingRunnable, 30);
            } else {
                typingRunnable = null;
                if (choiceButtons != null) {
                    choiceButtons.setVisibility(View.VISIBLE);
                }
            }
        };
        typingHandler.post(typingRunnable);
    }

    private LinearLayout.LayoutParams matchParentWrapContent() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }
}