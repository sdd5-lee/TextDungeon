package com.textdungeon.activity_control;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import com.example.textdungeon.R;
import com.textdungeon.ai.AiCallback;
import com.textdungeon.ai.AiType;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.event.GameEvent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DifficultyActivity extends BaseActivity {
    private AlertDialog loadingDialog;
    DataControlTower dt = DataControlTower.getInstance(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_difficulty);

        FrameLayout btnEasy = findViewById(R.id.btn_easy);
        FrameLayout btnNormal = findViewById(R.id.btn_normal);
        FrameLayout btnHard = findViewById(R.id.btn_hard);

        setSfx(btnEasy, btnNormal, btnHard);

        btnEasy.setOnClickListener(v -> {
            dt.setDifficulty("EASY");
            generateEventsAndStart(dt.getDifficulty().eventCount);
        });

        btnNormal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dt.setDifficulty("NORMAL");
                generateEventsAndStart(dt.getDifficulty().eventCount);
            }
        });

        btnHard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dt.setDifficulty("HARD");
                generateEventsAndStart(dt.getDifficulty().eventCount);
            }
        });
    }

    private void generateEventsAndStart(int targetCount) {
        loadingDialog = new AlertDialog.Builder(this)
                .setTitle("신들의 개입")
                .setMessage("운명을 창조하는 중입니다...\n(0 / " + targetCount + ")")
                .setCancelable(false)
                .create();
        loadingDialog.show();

        int[] completedCount = {0};

        List<AiType> shuffledGods = Arrays.asList(AiType.values());
        Collections.shuffle(shuffledGods);

        for (int i = 0; i < targetCount; i++) {
            AiType randomGod = shuffledGods.get(i % shuffledGods.size());
            int targetFloor = (i * 10)+1;
            String targetEventType = "normal";
            if (randomGod == AiType.STRUGGLE) {
                targetEventType = "battle";
            } else if (randomGod == AiType.TREASURE) {
                targetEventType = "shop";
            }
            dt.getAiManager().generate(
                    targetFloor,
                    dt.getPlayer().getStat(),
                    dt.getItemManager().getAll(),
                    targetEventType,
                    randomGod,
                    new AiCallback() {
                        @Override
                        public void onSuccess(GameEvent newEvent) {
                            runOnUiThread(() -> {
                                dt.addAiEvent(targetFloor, newEvent);

                                completedCount[0]++;
                                checkProgress(completedCount[0], targetCount);
                            });
                        }
                        @Override
                        public void onError(String errorMessage) {
                            runOnUiThread(() -> {
                                Log.e("DifficultyActivity", "이벤트 생성 실패: " + errorMessage);
                                completedCount[0]++;
                                checkProgress(completedCount[0], targetCount);
                            });
                        }
                    }
            );
        }
    }

    private void checkProgress(int current, int total) {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.setMessage("운명을 창조하는 중입니다...\n(" + current + " / " + total + ")");
            if (current >= total) {
                loadingDialog.dismiss();
                startGame();
            }
        }
    }

    private void startGame() {
        Intent intent = new Intent(this, EventActivity.class);
        startActivity(intent);
        finish();
    }

}