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

        // 🌟 버튼 ID를 기능별로 명확히 나누는 것을 추천합니다.
        // btn_start (새 게임), btn_continue (이어하기)

        View btnNewGame = findViewById(R.id.btn_start); // 기존 시작 버튼을 '새 게임'으로 활용
        View btnContinue = findViewById(R.id.btn_continue); // 레이아웃에 이어하기 버튼 추가 권장

        // 1. 새 게임 버튼: 무조건 캐릭터 생성창으로 이동
        btnNewGame.setOnClickListener(v -> moveCharacter());

        // 2. 이어하기 버튼 처리
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
        // 새 게임을 시작할 때는 기존 런 데이터를 지우고 가야 꼬이지 않습니다.
        // 만약 기존 데이터가 있다면 "정말 새로 시작하시겠습니까?" 팝업을 띄우는 것이 베스트입니다.
        if (dt.getPlayer() != null) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("새 게임 시작")
                    .setMessage("진행 중인 데이터가 삭제됩니다. 계속하시겠습니까?")
                    .setPositiveButton("예", (dialog, which) -> {
                        dt.resetRun(); // 진행 중인 모험 데이터만 초기화
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
        // 상점 (현재 테스트 레이아웃)
        startActivity(new Intent(this, TestLayout.class));
    }

    public void moveOption(View v) {
        // 옵션창 (현재 캐릭터 레이아웃으로 되어있는데, 나중에 전용 창을 만드세요!)
        Toast.makeText(this, "옵션 기능을 준비 중입니다.", Toast.LENGTH_SHORT).show();
    }
}