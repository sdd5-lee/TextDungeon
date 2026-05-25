package com.textdungeon.layout_control;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.textdungeon.R;
import com.textdungeon.data.DataControlTower;

public class MainActivity extends BaseActivity {
    private DataControlTower dt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.title_layout);

        dt = DataControlTower.getInstance(this);

        View btnNewGame = findViewById(R.id.btn_start);
        View btnContinue = findViewById(R.id.btn_continue);

        btnNewGame.setOnClickListener(v -> moveCharacter());

        if (btnContinue != null) {
            if (dt.getPlayer() != null) {
                btnContinue.setVisibility(View.VISIBLE);
                btnContinue.setOnClickListener(v -> moveEvent());
            } else {
                btnContinue.setVisibility(View.GONE);
            }
        }

        findViewById(R.id.btn_shop).setOnClickListener(this::moveShop);
        findViewById(R.id.btn_option).setOnClickListener(this::moveOption);
    }

    public void moveCharacter() {
        if (dt.getPlayer() != null) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("새 게임 시작")
                    .setMessage("진행 중인 데이터가 삭제됩니다. 계속하시겠습니까?")
                    .setPositiveButton("예", (dialog, which) -> {
                        dt.resetRun();
                        startActivity(new Intent(this, CharacterLayout.class));
                    })
                    .setNegativeButton("아니오", null)
                    .show();
        } else {
            startActivity(new Intent(this, CharacterLayout.class));
        }
    }

    public void moveEvent() {
        // 이어하기 로직
        Intent intent = new Intent(this, EventLayout.class);
        intent.putExtra("IS_NEW_GAME", false);
        startActivity(intent);
    }

    public void moveShop(View v) {
        startActivity(new Intent(this, ShopLayout.class));
    }

    public void moveOption(View v) {
        // 옵션창 (현재 캐릭터 레이아웃으로 되어있는데, 나중에 전용 창을 만드세요!)
        Toast.makeText(this, "옵션 기능을 준비 중입니다.", Toast.LENGTH_SHORT).show();
    }
}