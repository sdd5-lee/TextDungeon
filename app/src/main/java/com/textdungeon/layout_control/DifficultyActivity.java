package com.textdungeon.layout_control;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import com.example.textdungeon.R;
import com.google.android.material.snackbar.Snackbar;
import com.textdungeon.data.DataControlTower;

public class DifficultyActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DataControlTower dt = DataControlTower.getInstance(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_difficulty);

        FrameLayout btnEasy = findViewById(R.id.btn_easy);
        FrameLayout btnNormal = findViewById(R.id.btn_normal);
        FrameLayout btnHard = findViewById(R.id.btn_hard);

        btnEasy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dt.setDifficulty("EASY");
                startGame(v);
            }
        });

        btnNormal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dt.setDifficulty("NORMAL");
                startGame(v);
            }
        });

        btnHard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dt.setDifficulty("HARD");
                startGame(v);
            }
        });
    }
    private void startGame(View v) {
        Intent intent = new Intent(this, EventLayout.class);
        startActivity(intent);
    }
}