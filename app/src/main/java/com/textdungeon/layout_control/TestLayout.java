package com.textdungeon.layout_control;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.textdungeon.R;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.data.DungeonControl;
import com.textdungeon.event.BattleEvent;
import com.textdungeon.model.Item;
import com.textdungeon.model.Job;
import com.textdungeon.model.Magic;
import com.textdungeon.model.Monster;
import com.textdungeon.model.Stat;
import com.textdungeon.player.Player;
import com.textdungeon.system.GameSave;

import java.util.Map;

public class TestLayout extends AppCompatActivity {
    private DataControlTower dt;
    private Player player;

    private TextView logView, statusView, detailStatView, invView;
    private DungeonControl dungeonControl;

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
            dt.saveGame(this.dungeonControl);
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
        findViewById(R.id.btn_trigger_battle).setOnClickListener(v -> triggerEvent());

        findViewById(R.id.rebone).setOnClickListener(v -> {
            player.heal(player.getMaxHp());
            updateUI();
            addLog("시스템: 캐릭터를 부활시켰습니다");
        });
        findViewById(R.id.restart).setOnClickListener(v -> {
            dt.setPlayer(null);
            dt.initPlayer("player", Job.WARRIOR);
            player = dt.getPlayer();
            updateUI();
            addLog("시스템: 플레이어를 재생성합니다");
        });
        findViewById(R.id.die).setOnClickListener(v -> {
            addLog("시스템: 개발중 입니다");
        });
        findViewById(R.id.add_item_potion).setOnClickListener(v -> {
            Item p = dt.getItemManager().spawn("item_3");
            player.pickUpItem(p);
            updateUI();
            addLog("시스템: 포션을 추가하였습니다");
        });

        updateUI();
    }

    @Override
    protected void onResume() { // 돌아올 때마다 갱신
        super.onResume();
        updateUI();
    }

    private void triggerEvent() {
        BattleEvent event = dt.getEventManager().spawn("test_event");
        if (event != null) {
            String monsterId = event.getEnemyId();
            if (monsterId != null && !monsterId.isEmpty()) {
                startBattlePopup(event, monsterId);
            } else {
                // 몬스터 ID가 없음 -> 일반 텍스트 이벤트 실행!
                triggerEventNoBattle(event);
            }
        } else {
            addLog("시스템: 이벤트를 찾을 수 없습니다.");
        }
    }
    private void startBattlePopup(BattleEvent event, String monsterId) {
        Monster targetMonster = dt.getMonsterManager().spawn(monsterId);

        if (targetMonster != null) {
            addLog("\n[조우] " + event.getName());
            addLog(event.getDescription());

            // 전투 다이얼로그 호출
            BattleDialog battleDialog = new BattleDialog(this, player, targetMonster);

            // 다이얼로그가 닫히면(전투 종료 시) UI 갱신 및 사망 체크
            battleDialog.setOnDismissListener(dialog -> {
                updateUI();
                checkGameOver();
            });

            battleDialog.show();
        } else {
            addLog("\n시스템: 몬스터 [" + monsterId + "] 데이터를 찾을 수 없습니다.");
        }
    }
    private void checkGameOver() {
        if (player.getStat().getHp() <= 0) {
            addLog("\n💀 플레이어가 쓰러졌습니다...");
            // TODO: 데스 레이아웃으로 넘어가거나 영구적 죽음 처리 로직 추가
        }
    }
    private void triggerEventNoBattle(BattleEvent event) {
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
        String swordId = "item_5";
        if(!player.getInventory().isItem(swordId)){
            Item newItem = dt.getItemManager().spawn(swordId);
            Item newItem2 = dt.getItemManager().spawn("item_1");
            if (newItem != null && newItem2 != null){
                player.pickUpItem(newItem);
                player.pickUpItem(newItem2);

                player.equipItem(newItem);
                addLog("시스템: [" + newItem.getName() + "](를) 장착했습니다.");
            }
        }
        else {
            Item newItem = dt.getItemManager().spawn(swordId);
            if (newItem != null){
                player.equipItem(newItem);
                addLog("시스템: [" + newItem.getName() + "]을(를) 장착했습니다.");
            }
        }
        updateUI();
    }
    private void castMagic() {
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
        Magic masterData = dt.getMagicManager().spawn(magicId);

        if (masterData != null) {
            // MagicScroll에 새로 추가된 hasMagic과 addMagic 메서드 활용!
            if (!player.getMagicScroll().hasMagic(magicId)) {
                player.getMagicScroll().addMagic(magicId, masterData.getMaxCount());
                addLog("시스템: 새로운 마법 [" + masterData.getName() + "]을(를) 깨우쳤습니다!");
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
            Magic masterData = dt.getMagicManager().spawn(magicId);
            String magicName = (masterData != null) ? masterData.getName() : magicId;
            player.getMagicScroll().removeMagic(magicId);
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
                Item newItem = dt.getItemManager().spawn(entry.getKey());
                String itemName = newItem.getName();
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