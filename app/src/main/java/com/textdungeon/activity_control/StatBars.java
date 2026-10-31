package com.textdungeon.activity_control;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;

import com.example.textdungeon.R;

/**
 * 캐릭터 선택과 스탯 창에서 쓰는 막대 줄: 아이콘, 이름, 채운 막대, 숫자.
 * 막대는 전체를 max로 보고 채운 부분만 weight를 준다.
 */
public final class StatBars {
    public static final int RED = 0xFFD9534F;
    public static final int GREEN = 0xFF5CB85C;
    public static final int GOLD = 0xFFC9A24A;
    public static final int BLUE = 0xFF6B8FD6;
    private static final int LABEL = 0xFFB8B0A6;
    private static final int TEXT = 0xFFE5E2E1;

    private StatBars() {}

    /** 힘·민첩·체력·지혜 네 줄을 새로 그린다. 막대는 max에서 가득 찬다 */
    public static void show(LinearLayout parent, int strength, int agility, int health, int wisdom, int max) {
        parent.removeAllViews();
        addRow(parent, "힘", R.drawable.ic_strength, strength, max, RED, String.valueOf(strength));
        addRow(parent, "민첩", R.drawable.ic_dexterity, agility, max, GREEN, String.valueOf(agility));
        addRow(parent, "체력", R.drawable.ic_health, health, max, GOLD, String.valueOf(health));
        addRow(parent, "지혜", R.drawable.ic_wisdom, wisdom, max, BLUE, String.valueOf(wisdom));
    }

    /** 한 줄을 더한다. icon이 0이면 아이콘 없이, 숫자 칸은 valueText 길이만큼 */
    public static void addRow(LinearLayout parent, String label, @DrawableRes int icon,
                              int value, int max, int color, String valueText) {
        Context c = parent.getContext();
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(c, 3), 0, dp(c, 3));

        if (icon != 0) {
            ImageView iv = new ImageView(c);
            iv.setImageResource(icon);
            iv.setColorFilter(LABEL);
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(c, 14), dp(c, 14));
            iconLp.setMarginEnd(dp(c, 6));
            row.addView(iv, iconLp);
        }
        row.addView(text(c, label, 12, LABEL),
                new LinearLayout.LayoutParams(dp(c, 38), LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout track = new LinearLayout(c);
        track.setBackground(round(c, 0xFF2A2622));
        track.setClipToOutline(true);
        int full = Math.max(1, max);
        track.setWeightSum(full);
        View fill = new View(c);
        fill.setBackground(round(c, color));
        track.addView(fill, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT,
                Math.max(0, Math.min(value, full))));
        row.addView(track, new LinearLayout.LayoutParams(0, dp(c, 7), 1f));

        TextView num = text(c, valueText, 13, TEXT);
        num.setGravity(Gravity.END);
        num.setMinWidth(dp(c, 30));
        LinearLayout.LayoutParams numLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        numLp.setMarginStart(dp(c, 8));
        row.addView(num, numLp);
        parent.addView(row);
    }

    private static TextView text(Context c, String s, float sp, int color) {
        TextView tv = new TextView(c);
        tv.setText(s);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        tv.setTypeface(Typeface.SERIF);
        return tv;
    }

    private static GradientDrawable round(Context c, int color) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(c, 4));
        d.setColor(color);
        return d;
    }

    private static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }
}
