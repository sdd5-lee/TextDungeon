package com.textdungeon.layout_control;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.textdungeon.R;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.event.BattleEvent;
import com.textdungeon.model.Item;
import com.textdungeon.model.Stat;
import com.textdungeon.player.Player;
import com.textdungeon.system.GameSave;

import java.util.Map;

public class BattleLayout extends AppCompatActivity {
    private DataControlTower dt;
    private Player player;
    private GameSave gameSave;

    private TextView logView, statusView, detailStatView, invView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.battle_activity);

        dt = DataControlTower.getInstance(this);
        player = dt.player;

        if (player == null) {
            Toast.makeText(this, "에러 발생", Toast.LENGTH_SHORT).show();
            finish(); return;
        }

        gameSave = new GameSave(player);

        logView = findViewById(R.id.battle_log);
        statusView = findViewById(R.id.status_display);
        detailStatView = findViewById(R.id.stat_detail_text);
        invView = findViewById(R.id.inv_text);

        // 버튼 클릭 리스너 설정
        findViewById(R.id.btn_event).setOnClickListener(v -> triggerEvent());
        findViewById(R.id.btn_exp).setOnClickListener(v -> gainExp());
        findViewById(R.id.savebutton).setOnClickListener(v -> {
            gameSave.save(this);
            Toast.makeText(this, "저장 완료", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.loadbutton).setOnClickListener(v -> {
            GameSave loadedSave = GameSave.load(this);

            if (loadedSave != null && loadedSave.getPlayer() != null) {
                // 1. 불러온 플레이어 객체 추출
                Player loadedPlayer = loadedSave.getPlayer();

                // 2. 싱글톤 및 현재 화면의 플레이어 데이터 교체
                dt.player = loadedPlayer;
                this.player = loadedPlayer;

                // 3. [중요] 현재 액티비티의 gameSave 객체도 새 플레이어를 참조하도록 갱신
                // 이 과정이 없으면 로드 후 저장 시 데이터가 꼬이거나 초기화된 것처럼 보입니다.
                this.gameSave = new GameSave(this.player);

                updateUI();
                addLog("시스템: 데이터를 성공적으로 불러왔습니다.");
                Toast.makeText(this, "로드 완료!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "저장된 데이터를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        updateUI();
    }

    @Override
    protected void onResume() { // 돌아올 때마다 갱신
        super.onResume();
        updateUI();
    }

    private void triggerEvent() {
        // JSON 파일에 "event_1" 혹은 실제 있는 ID를 써야 합니다.
        BattleEvent event = dt.eventManager.spawn("event_1");

        if (event != null) {
            addLog("\n[이벤트] " + event.getName());
            addLog(event.getDescription());

            String result = event.execute(player, 0,dt.itemManager);
            addLog("결과: " + result);
            updateUI();
        } else {
            addLog("시스템: 이벤트를 찾을 수 없습니다.");
        }
    }

    private void gainExp() {
        player.getStat().gainStat("경험치", 50);
        player.levelUp();
        addLog("경험치 50 획득!");
        updateUI();
    }

    private void updateUI() {
        Stat s = player.getStat();
        statusView.setText(String.format("Lv: %d | HP: %d/%d | EXP: %d/%d",
                player.getLevel(), s.getHp(), s.getMaxHp(), s.getExp(), s.getMaxExp()));

        detailStatView.setText(String.format("힘:%d | 민첩:%d | 체력:%d | 지혜:%d\n(포인트:%d)",
                s.getStrength(), s.getAgility(), s.getHealth(), s.getWisdom(), s.getStatPoint()));

        StringBuilder sb = new StringBuilder("인벤토리: ");
        Map<String,Integer> itemMap =  player.getInventory().getItemMap();
        for (Map.Entry<String, Integer> entry : itemMap.entrySet()) {
            String itemName = entry.getKey();
            Integer count = entry.getValue();
            if (count != null) {
                sb.append(itemName).append("(").append(count).append("개) ");
            }
        }
        invView.setText(sb.toString());
    }

    private void addLog(String msg) {
        logView.append(msg + "\n");
    }
    public void battleMoveMain(android.view.View v) {
        finish();
    }
}