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

import java.util.ArrayList;
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
        FrameLayout btnNoAiEasy = findViewById(R.id.btn_no_ai_easy);

        setSfx(btnEasy, btnNormal, btnHard);

        btnEasy.setOnClickListener(v -> {
            dt.setDifficulty("EASY");
            generateEventsAndStart(dt.getDifficulty().eventCount);
        });
        btnNormal.setOnClickListener(v -> {
            dt.setDifficulty("NORMAL");
            generateEventsAndStart(dt.getDifficulty().eventCount);
        });
        btnHard.setOnClickListener(v -> {
            dt.setDifficulty("HARD");
            generateEventsAndStart(dt.getDifficulty().eventCount);
        });
        btnNoAiEasy.setOnClickListener(v -> {
            dt.setDifficulty("EASY");
            startGame();
        });
    }

    private void generateEventsAndStart(int targetCount) {
        loadingDialog = new AlertDialog.Builder(this)
                .setTitle("신들의 개입")
                .setMessage("운명을 창조하는 중입니다...\n(0 / " + targetCount + ")\n잠시 기다려 주십시오...")
                .setCancelable(false)
                .create();
        loadingDialog.show();

        int[] completedCount = {0};

        List<AiType> shuffledGods = new ArrayList<>(Arrays.asList(AiType.values()));
        Collections.shuffle(shuffledGods);

        for (int i = 0; i < targetCount; i++) {
            AiType randomGod = shuffledGods.get(i % shuffledGods.size());
            int targetFloor = (i * 10) + 1;
            String eventType = "normal";
            if (randomGod == AiType.STRUGGLE) eventType = "battle";
            else if (randomGod == AiType.TREASURE) eventType = "shop";

            final String finalEventType = eventType;
            final int finalFloor = targetFloor;
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() ->
                            generateWithRetry(finalFloor, randomGod, finalEventType, completedCount, targetCount, 0),
                    i * 3000L
            );
        }
    }

    private void generateWithRetry(int targetFloor, AiType randomGod, String eventType,
                                   int[] completedCount, int targetCount, int retryCount) {
        dt.getAiManager().generate(
                targetFloor,
                dt.getPlayer().getStat(),
                dt.getItemManager().getAll(),
                dt.getMonsterManager().getAll(),
                eventType,
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
                        Log.e("DifficultyActivity", "실패 (시도 " + (retryCount + 1) + "): " + errorMessage);
                        if (retryCount < 1) {
                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() ->
                                            generateWithRetry(targetFloor, randomGod, eventType,
                                                    completedCount, targetCount, retryCount + 1),
                                    15000L
                            );
                        } else {
                            runOnUiThread(() -> {
                                completedCount[0]++;
                                int failed = completedCount[0];
                                int success = failed - 1;
                                loadingDialog.setMessage("운명을 창조하는 중입니다...\n(" + success + " / " + targetCount + ") 실패 1\n잠시 기다려 주십시오...");
                                checkProgress(completedCount[0], targetCount);
                            });
                        }
                    }
                }
        );
    }

    private void checkProgress(int current, int total) {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.setMessage("운명을 창조하는 중입니다...\n(" + current + " / " + total + ")\n잠시 기다려 주십시오...");
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