package com.textdungeon.activity_control;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.textdungeon.R;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.model.Achievement;

import java.util.List;

public class MainActivity extends BaseActivity {
    private DataControlTower dt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dt = DataControlTower.getInstance(this);

        View btnNewGame = findViewById(R.id.btn_start);
        View btnContinue = findViewById(R.id.btn_continue);
        View btnShop = findViewById(R.id.btn_shop);
        View btnOption = findViewById(R.id.btn_option);

        setSfx(btnNewGame, btnShop, btnOption);

        btnNewGame.setOnClickListener(v -> moveCharacter());

        if (btnContinue != null) {
            setSfx(btnContinue);

            if (dt.getPlayer() != null) {
                btnContinue.setVisibility(View.VISIBLE);
                btnContinue.setOnClickListener(v -> moveEvent());
            } else {
                btnContinue.setVisibility(View.GONE);
            }
        }

        btnShop.setOnClickListener(this::moveShop);
        btnOption.setOnClickListener(this::moveOption);

        View btnAchievement = findViewById(R.id.btn_achievement);
        btnAchievement.setOnClickListener(v -> {
            setSfx(v);
            startActivity(new Intent(MainActivity.this, AchievementActivity.class));
        });
    }
    public void moveCharacter() {
        if (dt.getPlayer() != null) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("새 게임 시작")
                    .setMessage("진행 중인 데이터가 삭제됩니다. 계속하시겠습니까?")
                    .setPositiveButton("예", (dialog, which) -> {
                        dt.resetRun();
                        startActivity(new Intent(this, CharacterActivity.class));
                    })
                    .setNegativeButton("아니오", null)
                    .show();
        } else {
            startActivity(new Intent(this, CharacterActivity.class));
        }
    }

    public void moveEvent() {
        Intent intent = new Intent(this, EventActivity.class);
        intent.putExtra("IS_NEW_GAME", false);
        startActivity(intent);
    }

    public void moveShop(View v) {
        startActivity(new Intent(this, ShopActivity.class));
    }

    public void moveOption(View v) {
        startActivity(new Intent(this, SystemSettingActivity.class));
    }
}