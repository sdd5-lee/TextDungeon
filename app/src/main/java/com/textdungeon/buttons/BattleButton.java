package com.textdungeon.buttons;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import androidx.appcompat.widget.AppCompatButton;

import com.example.textdungeon.R;
import com.textdungeon.activity_control.GameUI;

public class BattleButton extends AppCompatButton {
    public BattleButton(Context context, String iconName, String itemName, int count, String actionName) {
        super(context);

        // [아이콘] 아이템명 (사용) 형식의 문자열 생성
        // GameUI를 사용하여 이미지와 텍스트가 섞인 Spannable 생성
        // 텍스트 크기에 맞춰 이미지가 렌더링됩니다.
        String fullText = "[" + iconName + "] " + itemName + " ("+count+")"+" (" + actionName + ")";

        this.setText(GameUI.parseRichText(context, fullText, this.getTextSize()));
        setTextColor(Color.parseColor("#E9C176"));
        setTextSize(12f);
        setTypeface(Typeface.SERIF, Typeface.BOLD);
        setBackgroundResource(R.drawable.bg_use_btn);
        setPadding(40, 10, 30, 10);
    }
}
