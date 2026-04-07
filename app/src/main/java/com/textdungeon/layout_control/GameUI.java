package com.textdungeon.layout_control;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.AlignmentSpan;
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
                    // 1. 이미지 크기 설정 (중앙에 오니 크기를 조금 더 키워봅니다. 예: 글자 크기의 2.0배)
                    int size = (int) (textSize * 5.0);
                    d.setBounds(0, 0, size, size);

                    // 2. ImageSpan 적용
                    ssb.setSpan(new ImageSpan(d, ImageSpan.ALIGN_BOTTOM), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                    // --- [중앙 정렬 로직 추가] ---

                    // 3. 이미지가 포함된 줄(paragraph)의 시작과 끝 위치 찾기
                    // 줄바꿈 문자(\n)를 기준으로 앞뒤 범위를 계산합니다.

                    // 줄 시작 위치 찾기
                    int lineStart = rawText.lastIndexOf("\n", start);
                    if (lineStart == -1) {
                        lineStart = 0; // 문자열 처음
                    } else {
                        lineStart += 1; // '\n' 다음 글자
                    }

                    // 줄 끝 위치 찾기
                    int lineEnd = rawText.indexOf("\n", end);
                    if (lineEnd == -1) {
                        lineEnd = rawText.length(); // 문자열 끝
                    }

                    // 4. AlignmentSpan을 사용하여 해당 줄을 중앙 정렬 (ALIGN_CENTER)
                    // 이 Span은 문단 단위(줄바꿈 기준)로 작동합니다.
                    ssb.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                            lineStart, lineEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
        return ssb;
    }
}
