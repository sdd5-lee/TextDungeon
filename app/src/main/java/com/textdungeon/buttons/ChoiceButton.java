package com.textdungeon.buttons;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import android.graphics.Typeface;
import com.example.textdungeon.R;

public class ChoiceButton extends FrameLayout {
    int choice;
    TextView textView;
    boolean buttonState = true;

    public ChoiceButton(Context context) {
        super(context);
        textView = new TextView(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        params.gravity = Gravity.CENTER;
        textView.setLayoutParams(params);
        textView.setGravity(Gravity.CENTER);

        setBackgroundResource(R.drawable.button_selector);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        textView.setTextColor(Color.parseColor("#E0BFBA"));
        textView.setTypeface(Typeface.SERIF, Typeface.BOLD);


        addView(textView);
    }
    public void setTextView(String text) {
        textView.setText(text);
    }

    public int getChoice() {
        return choice;
    }

    public void setChoice(int choice) {
        this.choice = choice;
    }

    public boolean getButtonState(){
        return buttonState;
    }
    public boolean setButtonState(boolean b) {
        buttonState = b;
        return buttonState;
    }
}
