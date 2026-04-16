package com.textdungeon.layout_control;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameUI {

    public static void setRichText(TextView textView, String rawText) {
        textView.setText(parseRichText(textView.getContext(), rawText, textView.getTextSize()));
    }

    public static SpannableStringBuilder parseRichText(Context context, String rawText, float textSize) {
        SpannableStringBuilder ssb = new SpannableStringBuilder(rawText);
        Pattern pattern = Pattern.compile("\\[(.*?)\\]");
        Matcher matcher = pattern.matcher(rawText);

        while (matcher.find()) {
            String tagName = matcher.group(1);
            int start = matcher.start();
            int end = matcher.end();

            int resId = context.getResources().getIdentifier(tagName, "drawable", context.getPackageName());

            if (resId != 0) {
                Drawable d = ContextCompat.getDrawable(context, resId);
                if (d != null) {
                    int size = (int) (textSize * 1.3);
                    d.setBounds(0, 0, size, size);

                    ssb.setSpan(new ImageSpan(d, ImageSpan.ALIGN_BOTTOM), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                }
            }
        }
        return ssb;
    }
}