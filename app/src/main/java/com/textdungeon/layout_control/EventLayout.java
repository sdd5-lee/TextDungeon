package com.textdungeon.layout_control;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.textdungeon.R;
import com.textdungeon.buttons.ChoiceButton;
import com.textdungeon.data.DataControl;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.data.DungeonControl;
import com.textdungeon.event.BattleEvent;
import com.textdungeon.model.Monster;
import com.textdungeon.player.Player;
import com.textdungeon.system.GameSave;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class EventLayout extends AppCompatActivity {
    DataControlTower dt;
    Player player;
    DungeonControl dungeonControl;
    BattleEvent currentEvent;
    TextView eventDesc;
    LinearLayout choiceButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_layout);

        dt = DataControlTower.getInstance(this);
        player = dt.getPlayer();

        dungeonControl = new DungeonControl();

        Intent intent = getIntent();
        boolean isNewGame = intent.getBooleanExtra("IS_NEW_GAME", true);
        if (!isNewGame) {
            GameSave loadedSave = GameSave.load(this);
            if (loadedSave != null) {
                dungeonControl.setCurrentFloor(loadedSave.getCurrentFloor());
            }
        }
        currentEvent = getRandomEvent();


        updateUI();
    }

    private BattleEvent getRandomEvent(){
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
        ImageView eventImage = findViewById(R.id.event_image);
        eventDesc = findViewById(R.id.event_description);
        choiceButtons = findViewById(R.id.choice_container);

        eventDesc.setText("");
        choiceButtons.removeAllViews();

        // 레벨, HP, 경험치 텍스트 업데이트
        levelText.setText("LV. " + player.getLevel());
        hpText.setText(player.getStat().getHp() + " / " + player.getMaxHp());
        xpText.setText(player.getStat().getExp() + " / " + player.getStat().getMaxExp());

        strText.setText(" " + player.getStat().getStrength());      // 공격력(힘)
        wisText.setText(" " + player.getStat().getWisdom());   // 지혜
        healthText.setText(" " + player.getStat().getHealth());// 체력 스탯
        agiText.setText(" " + player.getStat().getAgility());  // 민첩성

        floorText.setText(dungeonControl.getCurrentFloor()+" F");

        eventImage.setAlpha(1.0f);
        String imageName = currentEvent.getImgId();
        int imageResId = getResources().getIdentifier(imageName, "drawable", getPackageName());
        if (imageResId != 0) {
            eventImage.setImageResource(imageResId);
        } else {
            eventImage.setImageResource(R.drawable.mon_test);
        }
        addDesc(currentEvent.getName());
        addDesc(currentEvent.getDescription());

        int index = 0;
        for (String s : currentEvent.getChoices()) {
            ChoiceButton button = new ChoiceButton(this);
            button.setTextView(s);
            int finalIndex = index;
            button.setOnClickListener(v -> triggerEvent(finalIndex));
            choiceButtons.addView(button);
            index++;
        }

        ChoiceButton button = new ChoiceButton(this);
        button.setTextView("혼돈의 주사위 사용하기 ("+player.getDiceChane()+"개)");
        button.setOnClickListener(v -> ChaosEvent());
        choiceButtons.addView(button);
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
    private void resultEvent(int index) {
        updateUI();
        int playerLv = player.getLevel();
        String result = currentEvent.execute(player, index ,dt.getItemManager());
        addDesc("결과 : " + result);
        updateUIPlayer();

        if(playerLv < player.getLevel()){
            levelUpPopup();
        }else{
            nextFloor();
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

    private void ChaosEvent() {
        String result = currentEvent.execute(player, dt.getItemManager(),"준비중");
        addDesc(result);
        nextFloor();
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
            addDesc("\n시스템: 몬스터 [" + monsterId + "] 데이터를 찾을 수 없습니다.");
        }
    }
    private boolean checkGameOver() {
        //즉시 죽음 화면으로
        return false;
    }
    private void levelUpPopup(){
        addDesc("\n🎉 레벨업! " + player.getLevel() + "레벨이 되었습니다!");

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
        currentEvent = getRandomEvent();
        dt.saveGame(dungeonControl);
        ChoiceButton button = new ChoiceButton(this);
        button.setTextView("다음층으로");
        button.setOnClickListener(v -> updateUI());
        choiceButtons.addView(button);
    }

    private void addDesc(String text){
        eventDesc.append(" "+ text+"\n");
    }
}
