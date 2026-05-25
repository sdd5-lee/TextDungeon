package com.textdungeon.layout_control;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import com.example.textdungeon.R;

public class DifficultyActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_difficulty);

        FrameLayout btnEasy = findViewById(R.id.btn_easy);
        FrameLayout btnNormal = findViewById(R.id.btn_normal);
        FrameLayout btnHard = findViewById(R.id.btn_hard);

        btnEasy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Easy Mode
            }
        });

        btnNormal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Normal Mode
            }
        });

        btnHard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Hard Mode
            }
        });
    }
}