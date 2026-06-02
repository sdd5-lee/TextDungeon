package com.textdungeon.activity_control;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.example.textdungeon.R;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.event.GameEvent;
import com.textdungeon.model.Item;
import com.textdungeon.model.Monster;

public class AdminActivity extends BaseActivity {
    private DataControlTower dt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        dt = DataControlTower.getInstance(this);

        Button btnAddGold = findViewById(R.id.btn_admin_add_gold);
        Button btnUnlockAll = findViewById(R.id.btn_admin_unlock_all);
        Button btnLockAll = findViewById(R.id.btn_admin_lock_all);
        Button btnResetGame = findViewById(R.id.btn_admin_reset_game);
        Button btnClose = findViewById(R.id.btn_close_admin);

        setSfx(btnAddGold, btnUnlockAll, btnLockAll, btnResetGame, btnClose);

        btnAddGold.setOnClickListener(v -> {
            dt.getUserRecord().addGem(10000);

            Toast.makeText(this, "골드가 10,000 증가했습니다.", Toast.LENGTH_SHORT).show();
        });

        // 도감 전체 해금 버튼
        btnUnlockAll.setOnClickListener(v -> {
            for (Item item : dt.getItemManager().getAll()) {
                dt.getUserRecord().getDiscoveredItems().add(item.getId());
            }
            for (Monster mon : dt.getMonsterManager().getAll()) {
                dt.getUserRecord().getDiscoveredMonsters().add(mon.getId());
            }
            for (GameEvent ev : dt.getEventManager().getAll()) {
                dt.getUserRecord().getDiscoveredEvents().add(ev.getId());
            }
            Toast.makeText(this, "모든 도감 데이터가 해금되었습니다.", Toast.LENGTH_SHORT).show();
            dt.saveGame(); // 저장 필요시 주석 해제
        });

        btnLockAll.setOnClickListener(v -> {
            dt.getUserRecord().getDiscoveredItems().clear();
            dt.getUserRecord().getDiscoveredMonsters().clear();
            dt.getUserRecord().getDiscoveredEvents().clear();

            Toast.makeText(this, "도감 데이터가 초기화(잠금) 되었습니다.", Toast.LENGTH_SHORT).show();
            dt.saveGame(); // 저장 필요시 주석 해제
        });

        // 4. 게임 전체 초기화 버튼
        btnResetGame.setOnClickListener(v -> {
            dt.getUserRecord().clearAll();
            Toast.makeText(this, "게임 전체 데이터가 초기화되었습니다. 앱을 재시작하세요.", Toast.LENGTH_LONG).show();
        });

        // 5. 닫기 버튼
        btnClose.setOnClickListener(v -> finish());
    }
}