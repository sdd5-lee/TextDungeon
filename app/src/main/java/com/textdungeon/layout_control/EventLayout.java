package com.textdungeon.layout_control;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.textdungeon.R;
import com.textdungeon.ai.AiCallback;
import com.textdungeon.ai.ChaosManager;
import com.textdungeon.buttons.ChoiceButton;
import com.textdungeon.data.DataControl;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.data.DungeonControl;
import com.textdungeon.event.BattleEvent;
import com.textdungeon.model.Monster;
import com.textdungeon.player.Player;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class EventLayout extends BaseActivity {
    DataControlTower dt;
    Player player;
    DungeonControl dungeonControl;
    BattleEvent currentEvent;
    TextView eventDesc;
    LinearLayout choiceButtons;
    private boolean isDiceUsed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_layout);

        dt = DataControlTower.getInstance(this);
        player = dt.getPlayer();
        dungeonControl = dt.getDungeonControl();

        currentEvent = getRandomEvent();
        updateUI();

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                new android.app.AlertDialog.Builder(EventLayout.this)
                        .setTitle("게임 종료")
                        .setMessage("게임을 종료하시겠습니까?\n(현재 층수: " + dungeonControl.getCurrentFloor() + "F)")
                        .setPositiveButton("저장 후 종료", (dialog, which) -> {
                            dt.saveGame();
                            finishAffinity();
                        })
                        .setNegativeButton("취소", null)
                        .show();
            }
        });
    }
    private BattleEvent getRandomEvent(){
        isDiceUsed = false;
        int currentFloor = dungeonControl.getCurrentFloor();
        DataControl<BattleEvent> eventList = dt.getEventManager();
        List<BattleEvent> dataList = eventList.getAll();
        List<BattleEvent> possibleEvents = dataList.stream()
                .filter(e -> currentFloor>= e.getMinFloor() && currentFloor <= e.getMaxFloor())
                .collect(Collectors.toList());
        Random random = new Random();
        int index = random.nextInt(possibleEvents.size());
        return possibleEvents.get(index);
    }
    private void updateUI() {
        if (player == null) return;
        // [헤더] 플레이어 정보
        TextView levelText = findViewById(R.id.player_level);
        TextView hpText = findViewById(R.id.hp_text);
        TextView xpText = findViewById(R.id.xp_text);

        // [헤더] 스탯 정보
        TextView strText = findViewById(R.id.stat_str);
        TextView wisText = findViewById(R.id.stat_wis);
        TextView healthText = findViewById(R.id.stat_health);
        TextView agiText = findViewById(R.id.stat_agi);

        // [메인] 이벤트 화면
        TextView floorText = findViewById(R.id.floor_number);
        eventDesc = findViewById(R.id.event_description);
        choiceButtons = findViewById(R.id.choice_container);

        eventDesc.setText("");
        choiceButtons.removeAllViews();

        addDesc(currentEvent.getName());
        addDesc(currentEvent.getDescription());

        // 레벨, HP, 경험치 텍스트 업데이트
        levelText.setText("LV. " + player.getLevel());
        hpText.setText(player.getStat().getHp() + " / " + player.getMaxHp());
        xpText.setText(player.getStat().getExp() + " / " + player.getStat().getMaxExp());

        strText.setText(" " + player.getStat().getStrength());
        wisText.setText(" " + player.getStat().getWisdom());
        healthText.setText(" " + player.getStat().getHealth());
        agiText.setText(" " + player.getStat().getAgility());

        floorText.setText(dungeonControl.getCurrentFloor()+" F");

        ImageView eventImage = findViewById(R.id.event_image);
        eventImage.setAlpha(1.0f);

        String imageName = currentEvent.getImgId();
        int imageResId = getResources().getIdentifier(imageName, "drawable", getPackageName());
        eventImage.setImageResource(imageResId != 0 ? imageResId : R.drawable.mon_test);

        int index = 0;
        for (String s : currentEvent.getChoices()) {
            ChoiceButton button = new ChoiceButton(this);
            button.setTextView(s);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            button.setLayoutParams(params);
            int finalIndex = index;
            button.setOnClickListener(v -> triggerEvent(finalIndex));
            choiceButtons.addView(button);
            index++;
        }
        if (!isDiceUsed) {
            ChoiceButton diceButton = new ChoiceButton(this);
            diceButton.setTextView("혼돈의 주사위 사용하기 (" + player.getDiceChane() + "개)");
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            diceButton.setLayoutParams(params);
            diceButton.setOnClickListener(v -> ChaosEvent());
            choiceButtons.addView(diceButton);
        }
    }
    private void triggerEvent(int index) {
        if (currentEvent != null) {
            String monsterId = currentEvent.getEnemyId();
            choiceButtons.removeAllViews();
            if (monsterId != null && !monsterId.isEmpty()) {
                startBattlePopup(currentEvent, monsterId, index);
            } else {
                resultEvent(index);
            }
        } else {
            addDesc("시스템: 이벤트를 찾을 수 없습니다.");
        }
    }
    private void updateUIPlayer(){
        ((TextView) findViewById(R.id.player_level)).setText("LV. " + player.getLevel());
        ((TextView)findViewById(R.id.hp_text)).setText(player.getStat().getHp() + " / " + player.getMaxHp());
        ((TextView)findViewById(R.id.xp_text)).setText(player.getStat().getExp() + " / " + player.getStat().getMaxExp());
        ((TextView) findViewById(R.id.stat_str)).setText(" " + player.getStat().getStrength());
        ((TextView) findViewById(R.id.stat_wis)).setText(" " + player.getStat().getWisdom());
        ((TextView) findViewById(R.id.stat_health)).setText(" " + player.getStat().getHealth());
        ((TextView) findViewById(R.id.stat_agi)).setText(" " + player.getStat().getAgility());
    }

    private void resultEvent(int index) {
        int playerLv = player.getLevel();

        String result = currentEvent.execute(player, index ,dt.getItemManager());
        addDesc("결과 : " + result);


        updateUIPlayer();
        dt.saveGame();

        if(playerLv < player.getLevel()){
            levelUpPopup();
        }else{
            nextFloor();
        }
    }
    private void ChaosEvent() {
        if (player.getDiceChane() <= 0) return;
        android.util.Log.d("CHAOS_DEBUG", "1. 요청 시작"); // 🌟 출발 확인
        isDiceUsed = true;
        player.useDice();

        choiceButtons.removeAllViews();
        addDesc("🎲 운명을 재구성하는 중...");

        ChaosManager chaosManager = new ChaosManager(dt.getHttpClient());
        chaosManager.requestChaosChoice(
                dungeonControl.getCurrentFloor(),
                player.getStat(),
                dt.getItemManager().getAll(),
                currentEvent,
                new AiCallback() {
                    @Override
                    public void onSuccess(BattleEvent updatedEvent) {
                        runOnUiThread(() -> {
                            String desc;
                            if (updatedEvent != null && updatedEvent.getChoices().size() > 2) {
                                currentEvent = updatedEvent;
                                desc = "시스템: 새로운 선택지가 생성되었습니다.";
                            } else {
                                isDiceUsed = false;
                                desc = "시스템: 혼돈의 신이 응답하지 않습니다.";
                                player.addDiceChane(1);
                            }
                            updateUI();
                            addDesc(desc);
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        android.util.Log.e("CHAOS_FATAL", "진짜 에러 원인: " + errorMessage);
                        runOnUiThread(() -> {
                            isDiceUsed = false; // 에러 났으니 주사위 버튼 다시 살림
                            player.addDiceChane(1);
                            updateUI();
                            addDesc("에러발생");
                        });
                    }
                }
        );
    }
    private void startBattlePopup(BattleEvent event, String monsterId, int index) {
        Monster targetMonster = dt.getMonsterManager().spawn(monsterId);
        if (targetMonster != null) {
            // 전투 다이얼로그 호출
            BattleDialog battleDialog = new BattleDialog(this, player, targetMonster);

            // 다이얼로그가 닫히면(전투 종료 시) UI 갱신 및 사망 체크
            battleDialog.setOnDismissListener(dialog -> {
                if (!checkGameOver()) {
                    resultEvent(index);
                }
            });
            battleDialog.show();
        } else {
            addDesc("시스템: 몬스터 [" + monsterId + "] 데이터를 찾을 수 없습니다.");
        }
    }
    private boolean checkGameOver() {
        //즉시 죽음 화면으로
        return false;
    }
    private void levelUpPopup(){
        addDesc("레벨업! " + player.getLevel() + "레벨이 되었습니다!");

        StatDialog levelUpDialog = new StatDialog(this, player);

        levelUpDialog.setOnDismissListener(dialog -> {
            updateUI();
            nextFloor();
        });

        levelUpDialog.show();
    }

    private void nextFloor() {
        choiceButtons.removeAllViews();
        dungeonControl.nextCurrentFloor();
        dt.saveGame();
        ChoiceButton button = new ChoiceButton(this);

        button.setTextView("다음층으로 ("+dungeonControl.getCurrentFloor()+"F)");
        button.setOnClickListener(v -> {
            currentEvent = getRandomEvent();
            updateUI();
        });
        choiceButtons.addView(button);
    }

    private void addDesc(String text){
        eventDesc.append(" "+ text+"\n");
    }
}
