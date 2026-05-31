package com.textdungeon.dialog_control;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.textdungeon.R;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.model.Item;
import com.textdungeon.player.Player;

import java.util.Map;

public class PlayerInfoDialog extends Dialog {
    private final Player player;
    private final Context context;

    public PlayerInfoDialog(Context context, Player player) {
        super(context);
        this.context = context;
        this.player = player;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_player_info);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setCanceledOnTouchOutside(true);
        bind();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Window window = getWindow();
        if (window != null) {
            int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.90);
            int height = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.85);
            window.setLayout(width, height);
        }
    }

    private void bind() {
        DataControlTower dt = DataControlTower.getInstance(context);

        ((TextView) findViewById(R.id.info_name)).setText(player.getName());
        ((TextView) findViewById(R.id.info_job)).setText(player.getJob().name);
        ((TextView) findViewById(R.id.info_trait)).setText(player.getTrait().displayName);
        ((TextView) findViewById(R.id.info_level)).setText("LV. " + player.getLevel());
        ((TextView) findViewById(R.id.info_hp)).setText(
                player.getStat().getHp() + " / " + player.getMaxHp());
        ((TextView) findViewById(R.id.info_exp)).setText(
                player.getStat().getExp() + " / " + player.getStat().getMaxExp());
        ((TextView) findViewById(R.id.info_str)).setText("힘: " + player.getStat().getStrength());
        ((TextView) findViewById(R.id.info_agi)).setText("민첩: " + player.getStat().getAgility());
        ((TextView) findViewById(R.id.info_health)).setText("체력: " + player.getStat().getHealth());
        ((TextView) findViewById(R.id.info_wis)).setText("지혜: " + player.getStat().getWisdom());
        ((TextView) findViewById(R.id.info_atk)).setText("공격력: " + player.getStat().getAtk());
        ((TextView) findViewById(R.id.info_crit)).setText("크리티컬: " + player.getTotalCrit() + "%");
        ((TextView) findViewById(R.id.info_gold)).setText("골드: " + player.getStat().getGold());
        ((TextView) findViewById(R.id.info_stat_point)).setText("남은 스탯 포인트: " + player.getStat().getStatPoint());

        LinearLayout equipContainer = findViewById(R.id.info_equip_container);
        equipContainer.removeAllViews();
        for (Map.Entry<String, Integer> entry : player.getInventory().getItemMap().entrySet()) {
            Item item = dt.getItemManager().spawn(entry.getKey());
            if (item == null) continue;
            TextView tv = new TextView(context);
            tv.setTextColor(Color.parseColor("#E9C176"));
            tv.setTextSize(14f);
            tv.setPadding(0, 4, 0, 4);
            tv.setText("• " + item.getName()+ " " + entry.getValue()+" 개");
            equipContainer.addView(tv);
        }
    }
}