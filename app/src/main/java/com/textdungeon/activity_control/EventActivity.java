package com.textdungeon.activity_control;

import android.app.AlertDialog;
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
import com.textdungeon.ads.AdManager;
import com.textdungeon.ads.RewardType;
import com.textdungeon.ai.ChaosDice;
import com.textdungeon.buttons.ChoiceButton;
import com.textdungeon.buttons.SingleClickListener;
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
import com.textdungeon.model.Reward;
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
    // 주사위 응답을 기다리는 중. 이벤트에서 주사위를 더 쓸 수 있는지는 currentEvent.isChaosDone()으로 본다
    private boolean diceRolling = false;

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
        if (player == null) {
            // 진행 중인 게임이 없는데 들어온 경우 (정산 후 뒤로가기 등)
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
            return;
        }
        eventManager = new EventManager(dt);

        // 사망한 상태로 저장된 게임을 이어하면 HP 0으로 계속 진행되던 문제 방지
        if (eventManager.isPlayerDead()) {
            Intent intent = new Intent(this, DiedActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

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
            btnMagicLearn.setOnClickListener(v -> openMagicBook());
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

        GameImages.set(avatar, player.getJob().img.toLowerCase(), R.drawable.dungeon_entrance);

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

        // 상점은 어떤 선택지를 눌러도 상점이 열리므로 주사위를 띄우지 않는다
        if (!diceRolling && !currentEvent.isChaosDone()
                && !(currentEvent instanceof com.textdungeon.event.ShopEvent)) {
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

        boolean shown = GameImages.set(eventImage, currentEvent.getImgId(), 0);

        // 이벤트에 이미지가 없으면 전투 상대 몬스터 이미지
        if (!shown && currentEvent instanceof BattleEvent) {
            BattleEvent battleEvent = (BattleEvent) currentEvent;
            if (battleEvent.getEnemyId() != null) {
                Monster monster = eventManager.spawnMonster(battleEvent.getEnemyId());
                if (monster != null) {
                    shown = GameImages.set(eventImage, monster.getImgId(), 0);
                }
            }
        }

        if (!shown) eventImage.setImageResource(R.drawable.mon_god);
    }

    private void renderChoiceButtons() {
        int index = 0;
        for (String choiceText : currentEvent.getChoices()) {
            ChoiceButton button = new ChoiceButton(this);
            setSfx(button);

            button.setTextView(choiceText);
            button.setLayoutParams(matchParentWrapContent());
            int finalIndex = index;
            button.setOnClickListener(SingleClickListener.wrap(v -> onChoiceSelected(finalIndex)));
            choiceButtons.addView(button);
            index++;
        }
    }

    private void renderDiceButton() {
        ChoiceButton diceButton = new ChoiceButton(this);
        setSfx(diceButton);
        diceButton.setLayoutParams(matchParentWrapContent());
        // 남은 횟수가 0이고 이번 판에 아직 안 받았으면 광고로 +1 (판당 1번, 업그레이드 상한과 별개)
        if (player.getDiceChance() <= 0 && !player.isAdDiceUsed()) {
            bindAdDiceButton(diceButton);
        } else {
            bindDiceButton(diceButton);
        }
        choiceButtons.addView(diceButton);
    }

    private void bindDiceButton(ChoiceButton diceButton) {
        // 결과표 선택지가 나와서 주사위를 돌려받았으면 그 자리를 다시 굴릴 수 있다 (이벤트당 한 번)
        String label = currentEvent.getChaosSlot() != null ? "혼돈의 주사위 다시 굴리기" : "혼돈의 주사위 사용하기";
        diceButton.setTextView(label + " (" + player.getDiceChance() + "개)");
        diceButton.setOnClickListener(SingleClickListener.wrap(v -> onDiceSelected()));
    }

    private void bindAdDiceButton(ChoiceButton diceButton) {
        boolean skip = dt.getEntitlements().hasAdSkip();
        diceButton.setTextView(skip ? "혼돈의 주사위 +1 받기" : "광고 보고 혼돈의 주사위 +1");
        diceButton.setOnClickListener(SingleClickListener.wrap(v ->
                dt.getAdManager().showReward(this, RewardType.CHAOS_DICE, new AdManager.RewardCallback() {
                    @Override
                    public void onRewarded() {
                        if (isFinishing() || isDestroyed() || !player.grantAdDice()) return;
                        dt.saveGame(); // 받은 주사위와 '이번 판에 받음'을 같이 저장
                        bindDiceButton(diceButton);
                    }

                    @Override
                    public void onUnavailable(String message) {
                        showGameMessage(message);
                    }
                })));
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

        Reward selectedReward = currentEvent.getRewards().get(index);
        if ("battle".equals(selectedReward.getAction())) {
            if (currentEvent instanceof BattleEvent) {
                BattleEvent battleEvent = (BattleEvent) currentEvent;
                String monsterId = battleEvent.getEnemyId();

                if (monsterId != null && !monsterId.isEmpty()) {
                    showBattleDialog(monsterId, index);
                    return;
                }
            }
        }

        applyEventResult(index);
    }
    private void applyEventResult(int choiceIndex) {
        applyEventResult(choiceIndex, false);
    }

    private void applyEventResult(int choiceIndex, boolean giveUpItemIfFull) {
        int levelSnapshot = eventManager.snapshotLevel();
        String result = eventManager.applyReward(currentEvent, choiceIndex, giveUpItemIfFull);

        if (result.equals("full")) {
            appendDesc("인벤토리가 가득 찼습니다. 가방을 정리하거나 아이템을 포기하세요.");
            showInventoryFullOptions(choiceIndex);
            startTypingAnimation();
            return;
        }

        if (currentEvent != null && currentEvent.getId() != null) {
            dt.getUserRecord().getDiscoveredEvents().add(currentEvent.getId());

            int itemsCount = dt.getUserRecord().getDiscoveredItems().size();
            List<Achievement> unlocked = dt.getAchievementManager().updateProgress("collection_item", itemsCount, false);
            showAchievementNotification(unlocked);
        }
        appendDesc("결과 : " + result);
        updatePlayerHeader();

        // 이벤트 데미지로 HP가 0이 되면 전투가 아니어도 여기서 끝낸다 (예전엔 HP 0으로 다음 층에 진행됐음)
        if (eventManager.isPlayerDead()) {
            showDeathButton();
        }
        else if (currentEvent.isRetry(choiceIndex)) {
            renderRetryButtons();
        }
        else if (eventManager.didLevelUp(levelSnapshot)) {
            showLevelUpDialog();
        } else {
            showNextFloorButton();
        }
        startTypingAnimation();
    }

    /**
     * 가방이 가득 찼을 때의 선택지.
     * 예전에는 가방 창의 갱신 콜백마다 보상을 다시 적용해서 보상이 여러 번 들어가고 층이 넘어갔고,
     * 아무것도 버리지 않고 창을 닫으면 버튼이 없어 진행이 막혔다.
     * 이제 보상 적용은 창을 닫을 때 한 번만 시도하고, 아이템을 포기하는 선택지도 둔다.
     */
    private void showInventoryFullOptions(int choiceIndex) {
        choiceButtons.removeAllViews();

        ChoiceButton organize = new ChoiceButton(this);
        setSfx(organize);
        organize.setTextView("가방 정리하기");
        organize.setLayoutParams(matchParentWrapContent());
        organize.setOnClickListener(SingleClickListener.wrap(v -> {
            InventoryDialog dialog = new InventoryDialog(
                    EventActivity.this,
                    player,
                    dt.getItemManager(),
                    () -> runOnUiThread(this::updatePlayerHeader)
            );
            dialog.setOnDismissListener(d -> applyEventResult(choiceIndex));
            dialog.show();
        }));
        choiceButtons.addView(organize);

        ChoiceButton giveUp = new ChoiceButton(this);
        setSfx(giveUp);
        giveUp.setTextView("아이템은 포기하고 보상 받기");
        giveUp.setLayoutParams(matchParentWrapContent());
        giveUp.setOnClickListener(SingleClickListener.wrap(v -> applyEventResult(choiceIndex, true)));
        choiceButtons.addView(giveUp);
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
                button.setOnClickListener(SingleClickListener.wrap(v -> onChoiceSelected(finalIndex)));
                choiceButtons.addView(button);
            }
            index++;
        }
    }

    private void onDiceSelected() {
        // 응답을 기다리는 동안 다시 눌러도 요청이 또 나가지 않도록 진행 중이면 무시
        if (diceRolling || currentEvent.isChaosDone() || player.getDiceChance() <= 0) return;
        diceRolling = true;
        player.useDice();

        choiceButtons.removeAllViews();
        appendDesc("🎲 운명을 재구성하는 중...");
        startTypingAnimation();

        int floor = eventManager.getCurrentFloor();
        GameEvent rolledEvent = currentEvent;
        ChaosDice.Request request = new ChaosDice.Request(
                floor,
                player.getStat(),
                dt.getItemsForFloor(floor - 5, floor + 5), // 이 층대에 실제로 나오는 아이템만 (ChaosDice가 5개를 고른다)
                dt.getUpperGear(floor),                     // 특별 선택지의 장비 후보
                rolledEvent,
                dt.getChaosTable(),
                // 보상 요약을 실제 적용량으로 쓰기 위해 (GameEvent.execute: difficulty가 없으면 한 번만 적용)
                dt.getDifficulty() != null ? dt.getDifficulty().rewardMultiplier : 1,
                com.textdungeon.model.Reward.expMultiplier(player));
        dt.getAiManager().requestChaosChoice(request, new ChaosDice.Callback() {
            @Override
            public void onSuccess(GameEvent updatedEvent, ChaosDice.Source source, boolean refundDice) {
                runOnUiThread(() -> {
                    diceRolling = false;
                    if (refundDice) player.addDiceChane(1);
                    if (updatedEvent != currentEvent) return; // 그 사이 다음 층으로 넘어감
                    dt.getDungeonControl().setCurrentEvent(currentEvent);
                    String message;
                    if (source == ChaosDice.Source.SPECIAL) {
                        message = "시스템: 혼돈의 신이 미소 짓습니다. 특별한 선택지가 나타났습니다!";
                    } else if (refundDice) {
                        message = "시스템: 혼돈의 신이 졸고 있어 주사위가 돌아왔습니다. 한 번 더 굴려 볼 수 있습니다.";
                    } else {
                        message = "시스템: 새로운 선택지가 생성되었습니다.";
                    }
                    renderEvent(message);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    diceRolling = false;
                    player.addDiceChane(1);
                    if (rolledEvent == currentEvent) renderEvent("시스템: 혼돈의 신이 응답하지 않습니다.");
                });
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    // 팝업 / 다이얼로그
    // ─────────────────────────────────────────────────────────────

    /** 사망 문구와 "사망 확인" 버튼을 띄운다 (전투/이벤트 공통) */
    private void showDeathButton() {
        updatePlayerHeader();
        appendDesc("당신은 사망하였습니다.");
        choiceButtons.removeAllViews();

        ChoiceButton button = new ChoiceButton(this);
        setSfx(button);

        button.setTextView("사망 확인");
        button.setOnClickListener(SingleClickListener.wrap(v -> {
            Intent intent = new Intent(this, DiedActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }));
        choiceButtons.addView(button);
    }

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
                showDeathButton();
                startTypingAnimation();
            } else if(escapeState[0]) {
                applyEscapeResult();
            } else {
                dt.getUserRecord().getDiscoveredMonsters().add(monsterId);

                int monsterCount = dt.getUserRecord().getDiscoveredMonsters().size();
                List<Achievement> unlocked = dt.getAchievementManager().updateProgress("collection_monster", monsterCount, false);
                showAchievementNotification(unlocked);

                dt.getUserRecord().addKillCount();
                player.addRunKill(); // 정산용 이번 판 처치 수
                List<Achievement> killUnlocked = dt.getAchievementManager().updateProgress("kill", 1, true);
                showAchievementNotification(killUnlocked);

                updatePlayerHeader();
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

    private void openMagicBook() {
        new MagicLearnDialog(this, player, dt.getMagicManager()).show();
    }

    /** 이벤트나 스탯 분배가 끝났을 때(= 다음 층 버튼이 나올 때) 새로 얻은 마법 코어를 알린다 */
    private void notifyNewCores() {
        int fresh = player.getMagicScroll().takeNewCores(player.getStat().getWisdom());
        if (fresh <= 0) return;
        new android.app.AlertDialog.Builder(this)
                .setTitle("마법 코어 획득")
                .setMessage("코어 포인트를 " + fresh + "개 얻었습니다.\n마법서에서 새 마법 계열을 열 수 있습니다.")
                .setPositiveButton("마법서 열기", (d, w) -> openMagicBook())
                .setNegativeButton("닫기", null)
                .show();
    }

    private void showNextFloorButton() {
        eventManager.goNextFloor();
        choiceButtons.removeAllViews();

        int currentFloor = eventManager.getCurrentFloor();
        List<Achievement> unlocked = dt.getAchievementManager().updateProgress("floor", currentFloor, false);
        showAchievementNotification(unlocked);
        notifyNewCores();

        if (eventManager.getCurrentFloor() > 50){
            appendDesc("던전안에 숨어있던 마왕을 쓰러트렸습니다.\n더 이상의 적은 없다.");
            ChoiceButton button = new ChoiceButton(this);

            setSfx(button);

            button.setTextView("던전탐사를 끝낸다");
            button.setOnClickListener(SingleClickListener.wrap(v -> {
                Intent intent = new Intent(this, ClearActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                dt.saveGame();
                startActivity(intent);
                finish();
            }));
            choiceButtons.addView(button);
        } else {
            ChoiceButton button = new ChoiceButton(this);
            setSfx(button);

            button.setTextView("다음층으로 (" + eventManager.getCurrentFloor() + "F)");
            // 연타 시 두 번째 클릭이 방금 꺼낸 AI 이벤트를 랜덤 이벤트로 덮어쓰는 것 방지
            button.setOnClickListener(SingleClickListener.wrap(v -> {
                currentEvent = eventManager.pickRandomEvent();
                dt.getDungeonControl().setCurrentEvent(currentEvent);
                renderEvent();
            }));
            choiceButtons.addView(button);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 뒤로가기
    // ─────────────────────────────────────────────────────────────

    private void setupBackButton() {
        getOnBackPressedDispatcher().addCallback(this,
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        new AlertDialog.Builder(EventActivity.this)
                                .setTitle("게임 일시정지")
                                .setMessage("게임을 저장하시고 종료하시겠습니까?\n(현재 층수: "
                                        + eventManager.getCurrentFloor() + "F)")
                                .setPositiveButton("게임종료", (dialog, which) -> {
                                    dt.saveGame();
                                    // finishAffinity()는 프로세스를 끝내지 않으므로 모델은 따로 내린다
                                    dt.getAiManager().releaseOnDevice();
                                    finishAffinity();
                                })
                                .setNeutralButton("메인 화면으로",(dialog,which)->{
                                    dt.saveGame();
                                    // resetRun()을 거치지 않는 경로라 여기서 내린다. 이어하기를 하면 onResume에서 다시 올린다
                                    dt.getAiManager().releaseOnDevice();
                                    Intent intent = new Intent(EventActivity.this, MainActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                    startActivity(intent);
                                    finish();
                                })
                                .setNegativeButton("취소", null)
                                .show();
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────
    // 유틸
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

    @Override
    protected void onResume() {
        super.onResume();
        // 이어하기, 백그라운드에서 돌아올 때(메모리 부족으로 내렸을 수 있음)도 주사위용 모델을 올린다.
        // 이미 올라가 있으면 아무것도 하지 않는다
        if (player != null) dt.getAiManager().warmUpOnDevice();
    }

    @Override
    protected void onDestroy() {
        // 화면이 사라진 뒤에도 타이핑 애니메이션이 계속 도는 것 방지
        if (typingHandler != null) typingHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private LinearLayout.LayoutParams matchParentWrapContent() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }
}