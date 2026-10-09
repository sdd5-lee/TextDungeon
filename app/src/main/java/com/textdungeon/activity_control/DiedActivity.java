package com.textdungeon.activity_control;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.example.textdungeon.R;
import com.textdungeon.buttons.SingleClickListener;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.player.Player;
import com.textdungeon.system.GameSave;
import com.textdungeon.system.RunSettlement;

public class DiedActivity extends BaseActivity {
    private static final String KEY_GEMS = "earned_gems";

    private TextView tvClearDesc;
    private TextView tvScoreValue;
    private TextView btnExit;
    private int earnedGems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_died);

        tvClearDesc = findViewById(R.id.tv_clear_desc);
        tvScoreValue = findViewById(R.id.tv_score_value);
        btnExit = findViewById(R.id.btn_exit);

        // 화면이 다시 만들어질 때(회전 등) 젬이 또 지급되지 않도록, 정산은 처음 한 번만 하고 결과만 보관한다
        if (savedInstanceState != null) {
            earnedGems = savedInstanceState.getInt(KEY_GEMS, 0);
        } else {
            earnedGems = settleRun();
        }

        tvClearDesc.setText("당신의 여정은 여기서 끝이 났습니다.\n다시 일어나 새로운 여정을 시작하십시오.");
        tvScoreValue.setText(String.valueOf(earnedGems));

        setSfx(btnExit);

        btnExit.setOnClickListener(SingleClickListener.wrap(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_GEMS, earnedGems);
    }

    /**
     * 젬 지급 후 진행 중인 게임을 바로 삭제한다.
     * 예전엔 '나가기' 버튼을 눌러야 삭제돼서, 그 전에 앱을 끄면 이어하기로 다시 들어와 정산을 또 받을 수 있었다.
     */
    private int settleRun() {
        DataControlTower dt = DataControlTower.getInstance(this);
        Player player = dt.getPlayer();
        if (player == null) return 0; // 이미 정산됨

        int gems = RunSettlement.baseGems(dt, player);

        dt.getUserRecord().addGem(gems);
        GameSave.saveUserRecord(this, dt.getUserRecord());
        dt.resetRun();
        return gems;
    }
}