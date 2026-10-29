package com.textdungeon.activity_control;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.example.textdungeon.R;
import com.textdungeon.buttons.SingleClickListener;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.player.Player;
import com.textdungeon.system.GameSave;
import com.textdungeon.system.GemFormula;
import com.textdungeon.system.RunSettlement;

public class ClearActivity extends BaseActivity {
    private static final String KEY_GEMS = "earned_gems";

    private TextView tvClearDesc;
    private TextView tvScoreValue;
    private TextView btnExit;
    private int earnedGems;
    private SettlementAdBonus adBonus;
    DataControlTower dt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clear);

        tvClearDesc = findViewById(R.id.tv_clear_desc);
        tvScoreValue = findViewById(R.id.tv_score_value);
        btnExit = findViewById(R.id.btn_exit);

        dt = DataControlTower.getInstance(this);
        adBonus = new SettlementAdBonus(this, findViewById(R.id.btn_ad_bonus), bonus -> {
            earnedGems += bonus;
            tvScoreValue.setText(String.valueOf(earnedGems));
        });

        // 화면이 다시 만들어질 때(회전 등) 젬과 클리어 횟수가 또 올라가지 않도록 정산은 처음 한 번만
        if (savedInstanceState != null) {
            earnedGems = savedInstanceState.getInt(KEY_GEMS, 0);
            adBonus.restore(savedInstanceState);
        } else {
            earnedGems = settleRun();
        }

        // 정산(클리어 횟수 증가) 뒤에 읽어야 첫 클리어가 '0번'으로 표시되지 않는다
        int count = dt.getUserRecord().getClearCount();
        tvClearDesc.setText(String.format("당신은 신들의 축복을 받으며 던전 최심부에 도달하고\n 최종적으로 마왕을 쓰러트렸습니다 수고하셨습니다.\n\n당신은 지금까지 %d번 던전을 클리어했습니다.", count));
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
        adBonus.save(outState);
    }

    private int settleRun() {
        Player player = dt.getPlayer();
        if (player == null) return 0; // 이미 정산됨

        // (클리어 1000 * 난이도 + 기본 정산) × 젬 부스터. 광고 ×2는 버튼을 눌렀을 때 따로 준다
        int baseWithBonus = 1000 * dt.getDifficulty().rewardMultiplier + RunSettlement.baseGems(dt, player);
        int boost = dt.getEntitlements().gemBoostMultiplier();
        int gems = GemFormula.finalGems(baseWithBonus, boost, false);
        adBonus.settled(baseWithBonus, boost);

        dt.getUserRecord().addClearCount();
        dt.getUserRecord().addGem(gems);
        GameSave.saveUserRecord(this, dt.getUserRecord());
        dt.resetRun();
        return gems;
    }
}