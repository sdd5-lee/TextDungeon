package com.textdungeon.layout_control;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.textdungeon.MainActivity;
import com.example.textdungeon.R;
import com.textdungeon.data.DataControl;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.data.DungeonControl;
import com.textdungeon.event.BattleEvent;
import com.textdungeon.model.Item;
import com.textdungeon.model.Job;
import com.textdungeon.model.Stat;
import com.textdungeon.player.Player;
import com.textdungeon.system.GameSave;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class EventLayout extends AppCompatActivity {
    DataControlTower dt;
    Player player;
    DungeonControl dungeonControl;
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
        Random random = new Random();
        DataControl<BattleEvent> eventList = dt.getEventManager();
        List<BattleEvent> dataList = eventList.getAll();

        BattleEvent event = eventList.spawn("event_1");
        updateUI();
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
        TextView eventDesc = findViewById(R.id.event_description);

        // [선택지] 버튼들
        TextView choice1 = findViewById(R.id.choice1);
        TextView choice2 = findViewById(R.id.choice2);
        TextView choice3 = findViewById(R.id.choice3);
        // 레벨, HP, 경험치 텍스트 업데이트

        // 플레이어 데이터 꽂아넣기 (player 객체가 있다고 가정)
        levelText.setText("LV. " + player.getLevel());
        hpText.setText(player.getStat().getHp() + " / " + player.getMaxHp());

        xpText.setText(player.getStat().getExp() + " / " + player.getStat().getMaxExp());
        // 세부 스탯 업데이트
        strText.setText(" " + player.getStat().getAtk());      // 공격력(힘)
        wisText.setText(" " + player.getStat().getWisdom());   // 지혜
        healthText.setText(" " + player.getStat().getHealth());// 체력 스탯
        agiText.setText(" " + player.getStat().getAgility());  // 민첩성
        // 이벤트 상황 연출하기 (고블린을 만났을 때)

        floorText.setText(dungeonControl.getCurrentFloor()+" F");
        eventImage.setImageResource(R.drawable.mon_goblin); // res/drawable/mon_goblin.png 파일 필요
        eventImage.setAlpha(1.0f); // XML에서 투명도가 0.4로 되어있어서 잘 안 보일 수 있으니 진하게!

        eventDesc.setText("음습한 통로를 걷던 중, 횃불 빛에 반사된 번뜩이는 눈동자와 마주쳤다.\n\n굶주린 고블린이 낡은 단검을 들고 길을 막아선다.");

        // 선택지 세팅하기1
        choice1.setText("전투를 준비한다");
        choice2.setText("조용히 뒷걸음질 친다 (회피)");
        choice3.setText("가방을 열어 아이템을 찾는다");
    }
}
