package com.textdungeon.layout_control;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.textdungeon.R;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.event.BattleEvent;
import com.textdungeon.model.Item;
import com.textdungeon.model.Magic;
import com.textdungeon.model.Stat;
import com.textdungeon.player.Player;
import com.textdungeon.system.GameSave;

import java.util.Map;

public class TestLayout extends AppCompatActivity {
    private DataControlTower dt;
    private Player player;

    private TextView logView, statusView, detailStatView, invView;

    boolean isWeaponA = true;;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_activity);

        dt = DataControlTower.getInstance(this);
        player = dt.getPlayer();

        if (player == null) {
            Toast.makeText(this, "에러 발생", Toast.LENGTH_SHORT).show();
            finish(); return;
        }


        logView = findViewById(R.id.battle_log);
        statusView = findViewById(R.id.status_display);
        detailStatView = findViewById(R.id.stat_detail_text);
        invView = findViewById(R.id.inv_text);

        // 버튼 클릭 리스너 설정
        findViewById(R.id.btn_event).setOnClickListener(v -> triggerEvent());
        findViewById(R.id.btn_exp).setOnClickListener(v -> gainExp());
        findViewById(R.id.savebutton).setOnClickListener(v -> {
            dt.saveGame();
            Toast.makeText(this, "저장 완료", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.loadbutton).setOnClickListener(v -> {
            GameSave loadedSave = GameSave.load(this);

            if (loadedSave != null && loadedSave.getPlayer() != null) {
                dt.setPlayer(loadedSave.getPlayer());
                if (loadedSave.getUserRecord() != null) {
                    dt.setUserRecord(loadedSave.getUserRecord());
                }

                this.player = dt.getPlayer();

                updateUI();
                addLog("시스템: 데이터를 성공적으로 불러왔습니다.");
                Toast.makeText(this, "로드 완료!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "저장된 데이터를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.btn_swap).setOnClickListener(v -> swapWeapon());

        findViewById(R.id.btn_cast).setOnClickListener(v -> castMagic());

        findViewById(R.id.btn_learn).setOnClickListener(v -> learnMagic());

        findViewById(R.id.btn_forget).setOnClickListener(v -> forgetMagic());
        updateUI();
    }

    @Override
    protected void onResume() { // 돌아올 때마다 갱신
        super.onResume();
        updateUI();
    }

    private void triggerEvent() {
        // JSON 파일에 "event_1" 혹은 실제 있는 ID를 써야 합니다.
        BattleEvent event = dt.getEventManager().spawn("event_1");

        if (event != null) {
            addLog("\n[이벤트] " + event.getName());
            addLog(event.getDescription());

            String result = event.execute(player, 0,dt.getItemManager());
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
    private void swapWeapon() {
        // isWeaponA가 true면 item_1(검), false면 item_4(낡은 책) 생성
        String targetId = isWeaponA ? "item_1" : "item_4";
        Item newItem = dt.getItemManager().spawn(targetId);

        if (newItem != null) {
            player.equipItem(newItem);
            addLog("시스템: [" + newItem.getName() + "]을(를) 장착했습니다.");
            isWeaponA = !isWeaponA; // 상태 토글
            updateUI();
        } else {
            addLog("시스템: 해당 무기 데이터를 찾을 수 없습니다.");
        }
    }private void castMagic() {
        String magicId = "mag_1";
        int damage = player.castMagic(magicId, this);

        if (damage > 0) {
            addLog("[전투] 마법 시전 성공! 적에게 " + damage + "의 데미지를 입혔습니다.");
        } else {
            addLog("[전투] 마법 시전 실패! (배우지 않았거나 사용 횟수가 부족합니다.)");
        }
        updateUI();
    }
    private void learnMagic() {
        String magicId = "mag_1";
        String magicId2 = "mag_2";
        Magic masterData = dt.getMagicManager().spawn(magicId);
        Magic masterData2 = dt.getMagicManager().spawn(magicId2);

        if (masterData != null) {
            // MagicScroll에 새로 추가된 hasMagic과 addMagic 메서드 활용!
            if (!player.getMagicScroll().hasMagic(magicId)) {
                player.getMagicScroll().addMagic(magicId, masterData.getMaxCount());
                player.getMagicScroll().addMagic(magicId2, masterData.getMaxCount());
                addLog("시스템: 새로운 마법 [" + masterData.getName() + "]을(를) 깨우쳤습니다!");
                addLog("시스템: 새로운 마법 [" + masterData2.getName() + "]을(를) 깨우쳤습니다!");
            } else {
                addLog("시스템: 이미 배운 마법입니다.");
            }
        } else {
            addLog("시스템: 마법 데이터를 찾을 수 없습니다.");
        }
    }

    private void forgetMagic() {
        String magicId = "mag_1";

        if (player.getMagicScroll().hasMagic(magicId)) {
            // 1. 이름을 알아내기 위해 타워에서 마스터 데이터를 잠시 빌려옵니다.
            Magic masterData = dt.getMagicManager().spawn(magicId);

            // 안전장치: 혹시라도 데이터가 없으면 그냥 ID를 출력하고, 있으면 진짜 이름을 씁니다.
            String magicName = (masterData != null) ? masterData.getName() : magicId;

            // 2. 마법 삭제 로직 (기존과 동일)
            player.getMagicScroll().getLearnedMagicList().removeIf(lm -> lm.getMagicId().equals(magicId));

            // 3. 드디어 ID가 아닌 '진짜 이름'으로 로그 출력!
            addLog("시스템: 마법 [" + magicName + "]의 기억을 지웠습니다.");
        } else {
            addLog("시스템: 지울 마법이 없습니다. (아직 배우지 않음)");
        }
    }
    private void updateUI() {
        Stat s = player.getStat();

        // 레벨, HP, 경험치 텍스트 업데이트
        statusView.setText(String.format("Lv: %d | HP: %d/%d | EXP: %d/%d",
                player.getLevel(), s.getHp(), player.getMaxHp(), s.getExp(), s.getMaxExp()));

        // 세부 스탯 업데이트
        detailStatView.setText(String.format("힘:%d | 민첩:%d | 체력:%d | 지혜:%d\n(포인트:%d)",
                s.getStrength(), s.getAgility(), s.getHealth(), s.getWisdom(), s.getStatPoint()));

        // 인벤토리 목록 업데이트
        StringBuilder sb = new StringBuilder("인벤토리: ");
        Map<String, Integer> itemMap = player.getInventory().getItemMap();

        if (itemMap.isEmpty()) {
            sb.append("비어 있음");
        } else {
            for (Map.Entry<String, Integer> entry : itemMap.entrySet()) {
                String itemName = entry.getKey();
                Integer count = entry.getValue();
                if (count != null && count > 0) {
                    sb.append(itemName).append("(").append(count).append("개) ");
                }
            }
        }
        invView.setText(sb.toString());
    }
    private void addLog(String msg) {
        // 기존처럼 아래에 텍스트를 이어 붙입니다.
        logView.append(GameUI.parseRichText(this, msg + "\n", logView.getTextSize()));

        // UI 업데이트가 끝난 직후(post)에 스크롤을 맨 아래로 내리라는 명령을 예약합니다.
        logView.post(new Runnable() {
            @Override
            public void run() {
                // xml에 ScrollView가 있고, 그 아이디가 R.id.log_scroll 일 경우
                android.widget.ScrollView scrollView = findViewById(R.id.log_scroll);
                if (scrollView != null) {
                    scrollView.fullScroll(View.FOCUS_DOWN);
                }
            }
        });
    }
    public void battleMoveMain(android.view.View v) {
        finish();
    }
}