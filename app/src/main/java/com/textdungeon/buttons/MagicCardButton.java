package com.textdungeon.buttons;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.textdungeon.R;
import com.textdungeon.activity_control.GameImages;
import com.textdungeon.model.LearnedMagic;
import com.textdungeon.model.Magic;

/**
 * 전투 창 마법 카드. 마법 그림이 버튼 전체를 채우고, 테두리는 계열 색,
 * 오른쪽 위에 남은 횟수, 아래 띠에 이름. 다 쓰면 흑백이 된다.
 */
public class MagicCardButton extends FrameLayout {

    private static final int TEXT = 0xFFF0E2C0;
    private static final int TEXT_EMPTY = 0xFFA8A296;
    private static final int BORDER_EMPTY = 0xFF5F5A52;

    private final float density;

    public MagicCardButton(Context context, Magic magic, LearnedMagic learned) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        boolean empty = learned.getCurrentCount() <= 0;
        String countText = learned.getCurrentCount() + "/" + learned.getMaxCount();

        // 둥근 모서리 밖으로 그림·띠가 나오지 않게 바탕 모양대로 자른다
        setBackground(rounded(0xFF1A1612));
        setClipToOutline(true);

        ImageView image = new ImageView(context);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GameImages.set(image, magic.getImg(), R.drawable.magic_default_icon);
        if (empty) {
            ColorMatrix gray = new ColorMatrix();
            gray.setSaturation(0f);
            image.setColorFilter(new ColorMatrixColorFilter(gray));
            image.setImageAlpha(140);
        }
        addView(image, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        TextView name = new TextView(context);
        name.setText(magic.getName());
        name.setTextSize(12f);
        name.setTypeface(Typeface.SERIF, Typeface.BOLD);
        name.setTextColor(empty ? TEXT_EMPTY : TEXT);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setBackgroundColor(0x99000000);
        name.setPadding(dp(8), dp(2), dp(8), dp(3));
        addView(name, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));

        TextView count = new TextView(context);
        count.setText(countText);
        count.setTextSize(11f);
        count.setTypeface(Typeface.SERIF, Typeface.BOLD);
        count.setTextColor(empty ? TEXT_EMPTY : TEXT);
        GradientDrawable badge = new GradientDrawable();
        badge.setCornerRadius(dp(8));
        badge.setColor(0xA6000000);
        count.setBackground(badge);
        count.setPadding(dp(6), dp(1), dp(6), dp(1));
        LayoutParams countParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        countParams.setMargins(dp(4), dp(4), dp(4), dp(4));
        addView(count, countParams);

        // 테두리와 누를 때 번쩍임은 그림 위에 겹쳐 그린다
        GradientDrawable border = rounded(0x00000000);
        border.setStroke(dp(2), empty ? BORDER_EMPTY : magic.getSchool().color);
        RippleDrawable ripple = new RippleDrawable(ColorStateList.valueOf(0x40FFFFFF), null, rounded(0xFFFFFFFF));
        setForeground(new LayerDrawable(new Drawable[]{ripple, border}));

        setClickable(true);
        setFocusable(true);
        setContentDescription(magic.getName() + " " + countText);
    }

    private GradientDrawable rounded(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(6));
        d.setColor(color);
        return d;
    }

    private int dp(float v) {
        return Math.round(v * density);
    }
}
