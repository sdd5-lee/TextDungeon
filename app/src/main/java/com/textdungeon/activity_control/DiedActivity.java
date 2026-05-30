package com.textdungeon.activity_control;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.example.textdungeon.R;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.model.Item;
import com.textdungeon.model.Stat;
import com.textdungeon.player.Player;

public class DiedActivity extends BaseActivity {
    private TextView tvClearDesc;
    private TextView tvScoreValue;
    private TextView btnRestart;
    private TextView btnExit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_died);

        tvClearDesc = findViewById(R.id.tv_clear_desc);
        tvScoreValue = findViewById(R.id.tv_score_value);
        btnRestart = findViewById(R.id.btn_restart);
        btnExit = findViewById(R.id.btn_exit);

        tvClearDesc.setText("당신의 여정은 여기서 끝이 났습니다.\n다시 일어나 새로운 여정을 시작하십시오.");
        tvScoreValue.setText(String.valueOf(diedGem()));

        setSfx(btnRestart, btnExit);
        btnRestart.setOnClickListener(v -> {
            DataControlTower.getInstance(this).resetRun();
            Intent intent = new Intent(this, CharacterActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        btnExit.setOnClickListener(v -> {
            DataControlTower.getInstance(this).resetRun();
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private int diedGem() {
        DataControlTower dt = DataControlTower.getInstance(this);
        Player player = dt.getPlayer();
        Stat stat = player.getStat();

        if (player == null) return 0; // 추가
        int gems = 0;

        //스텟 총합 +레벨 +돈
        gems = gems+(stat.getStrength()+stat.getWisdom()+stat.getHealth()+stat.getAgility()+stat.getGold()+player.getLevel());

        //아이템 밸류합
        for (String s : player.getInventory().getItemMap().keySet()) {
            Item i = dt.getItemManager().spawn(s);
            gems += i.getValue();
        }

        //배운 마법당 10
        gems += player.getMagicScroll().getLearnedMagics().size()*10;

        //몬스터 처리한 횟수
        gems += dt.getUserRecord().getKillCount();

        dt.getUserRecord().addGem(gems);
        return gems;
    }
}