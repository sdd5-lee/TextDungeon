package com.textdungeon.activity_control;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.example.textdungeon.R;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.model.Item;
import com.textdungeon.model.Stat;
import com.textdungeon.player.Player;

public class ClearActivity extends BaseActivity {
    private TextView tvClearDesc;
    private TextView tvScoreValue;
    private TextView btnExit;
    DataControlTower dt = DataControlTower.getInstance(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clear);

        tvClearDesc = findViewById(R.id.tv_clear_desc);
        tvScoreValue = findViewById(R.id.tv_score_value);

        btnExit = findViewById(R.id.btn_exit);

        int count = dt.getUserRecord().getClearCount();
        tvClearDesc.setText(String.format("당신은 신들의 축복을 받으며 던전 최심부에 도달하고\n 최종적으로 마왕을 쓰러트렸습니다 수고하셨습니다.\n\n당신은 지금까지 %d번 던전을 클리어했습니다.",count));
        tvScoreValue.setText(String.valueOf(clearGem()));

        setSfx(btnExit);

        btnExit.setOnClickListener(v -> {
            DataControlTower.getInstance(this).resetRun();
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

    }

    private int clearGem() {
        int gems = 0;
        Player player = dt.getPlayer();
        Stat stat = player.getStat();

        //클리어 했으니까 1000 * 난이도
        gems = 1000*dt.getDifficulty().rewardMultiplier;

        //스텟 총합 + 레벨 + 돈
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

        dt.getUserRecord().addClearCount();
        dt.getUserRecord().addGem(gems);
        return gems;
    }
}