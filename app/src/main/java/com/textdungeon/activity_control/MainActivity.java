package com.textdungeon.activity_control;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.textdungeon.R;
import com.textdungeon.buttons.SingleClickListener;
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
        // 결제: 연결되면 보유 상품을 Play 목록으로 맞춘다 (실패하면 저장된 캐시로 계속)
        // 광고: 보상형 광고를 미리 불러 둔다 (광고 스킵을 보유하면 광고 SDK를 켜지 않는다)
        dt.getBillingManager().start();
        dt.getAdManager().initialize();

        View btnNewGame = findViewById(R.id.btn_start);
        View btnContinue = findViewById(R.id.btn_continue);
        View btnShop = findViewById(R.id.btn_shop);
        View btnOption = findViewById(R.id.btn_option);

        setSfx(btnNewGame, btnShop, btnOption);

        btnNewGame.setOnClickListener(v -> moveCharacter());

        if (btnContinue != null) {
            setSfx(btnContinue);
            btnContinue.setOnClickListener(SingleClickListener.wrap(v -> moveEvent()));
        }

        btnShop.setOnClickListener(this::moveShop);
        btnOption.setOnClickListener(this::moveOption);

        View btnAchievement = findViewById(R.id.btn_achievement);
        btnAchievement.setOnClickListener(v -> {
            setSfx(v);
            startActivity(new Intent(MainActivity.this, AchievementActivity.class));
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        // 게임 중 '메인 화면으로' 돌아오거나 정산 후 돌아왔을 때도 이어하기 버튼이 맞게 보이도록 매번 갱신
        View btnContinue = findViewById(R.id.btn_continue);
        if (btnContinue != null) {
            btnContinue.setVisibility(dt.getPlayer() != null ? View.VISIBLE : View.GONE);
        }
        // 혼돈의 주사위 온디바이스 모델이 없으면 Wi-Fi에서 받기 시작한다 (받는 동안은 클라우드)
        dt.getAiManager().startModelDownload();
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
        startActivityForResult(new Intent(this, SystemSettingActivity.class), 100);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            recreate();
        }
    }
}