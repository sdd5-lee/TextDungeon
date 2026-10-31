package com.textdungeon.dialog_control;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.textdungeon.R;
import com.textdungeon.activity_control.GameImages;
import com.textdungeon.activity_control.StatBars;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.model.Item;
import com.textdungeon.model.Stat;
import com.textdungeon.model.Trait;
import com.textdungeon.player.Player;

import java.util.Map;

/**
 * 스탯 창. 캐릭터 선택 화면처럼 직업 초상화 위에 레벨·이름·특성 배지를 얹고,
 * 아래에 HP·경험치 막대, 능력치 막대, 전투·골드 칸, 특성 카드, 가방을 보여 준다.
 */
public class PlayerInfoDialog extends Dialog {
    /** 능력치 막대가 가득 차는 최소값 (캐릭터 선택과 같다). 더 높은 능력치가 있으면 5 단위로 올려 잡는다 */
    private static final int STAT_MAX = 15;

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
        // 초상화가 둥근 모서리 밖으로 나가지 않게
        findViewById(R.id.info_root).setClipToOutline(true);
        findViewById(R.id.btn_info_close).setOnClickListener(v -> dismiss());
        bind();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Window window = getWindow();
        if (window != null) {
            int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.92);
            int height = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.88);
            window.setLayout(width, height);
        }
    }

    private void bind() {
        Stat stat = player.getStat();
        GameImages.set(findViewById(R.id.info_portrait), player.getJob().img, android.R.drawable.ic_menu_gallery);
        ((TextView) findViewById(R.id.info_level)).setText("LV. " + player.getLevel() + " · " + player.getJob().name);
        ((TextView) findViewById(R.id.info_name)).setText(player.getName());

        LinearLayout gauges = findViewById(R.id.info_gauges);
        int maxHp = player.getMaxHp();
        StatBars.addRow(gauges, "HP", 0, stat.getHp(), maxHp, StatBars.RED, stat.getHp() + " / " + maxHp);
        StatBars.addRow(gauges, "경험치", 0, stat.getExp(), stat.getMaxExp(), StatBars.BLUE,
                stat.getExp() + " / " + stat.getMaxExp());

        int highest = Math.max(Math.max(stat.getStrength(), stat.getAgility()), Math.max(stat.getHealth(), stat.getWisdom()));
        int barMax = Math.max(STAT_MAX, (highest + 4) / 5 * 5);
        StatBars.show(findViewById(R.id.info_stat_bars),
                stat.getStrength(), stat.getAgility(), stat.getHealth(), stat.getWisdom(), barMax);

        ((TextView) findViewById(R.id.info_atk)).setText(String.valueOf(player.getFinalAtk()));
        ((TextView) findViewById(R.id.info_crit)).setText(player.getTotalCrit() + "%");
        ((TextView) findViewById(R.id.info_gold)).setText(String.valueOf(stat.getGold()));
        ((TextView) findViewById(R.id.info_stat_point)).setText(String.valueOf(stat.getStatPoint()));

        TextView badge = findViewById(R.id.info_trait_badge);
        Trait trait = player.getTrait();
        if (trait == null) {
            badge.setVisibility(View.GONE);
            findViewById(R.id.info_trait_section).setVisibility(View.GONE);
        } else {
            badge.setText("특성 · " + trait.displayName);
            ((TextView) findViewById(R.id.info_trait_name)).setText(trait.displayName);
            ((TextView) findViewById(R.id.info_trait_description)).setText(trait.description);
        }

        showItems();
    }

    /** 가방에 든 물건을 이름 왼쪽, 개수 오른쪽으로 한 줄씩. 없으면 "비어 있음" */
    private void showItems() {
        DataControlTower dt = DataControlTower.getInstance(context);
        LinearLayout list = findViewById(R.id.info_equip_container);
        list.removeAllViews();
        for (Map.Entry<String, Integer> entry : player.getInventory().getItemMap().entrySet()) {
            Item item = dt.getItemManager().spawn(entry.getKey());
            if (item == null || entry.getValue() <= 0) continue;
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(6), 0, dp(6));
            row.addView(text(item.getName(), 0xFFE5E2E1),
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView count = text("× " + entry.getValue(), 0xFFE9C176);
            count.setGravity(Gravity.END);
            row.addView(count);
            list.addView(row);
        }
        if (list.getChildCount() == 0) {
            TextView empty = text("비어 있음", 0xFF8D867C);
            empty.setPadding(0, dp(6), 0, dp(6));
            list.addView(empty);
        }
    }

    private TextView text(String s, int color) {
        TextView tv = new TextView(context);
        tv.setText(s);
        tv.setTextSize(14);
        tv.setTextColor(color);
        tv.setTypeface(Typeface.SERIF);
        return tv;
    }

    private int dp(float v) {
        return Math.round(v * context.getResources().getDisplayMetrics().density);
    }
}
