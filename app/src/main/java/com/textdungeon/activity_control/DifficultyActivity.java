package com.textdungeon.activity_control;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
    private static final String TAG = "DifficultyActivity";
    private static final int MAX_RETRY = 1;
    private static final long RETRY_DELAY_MS = 2000L;

    private AlertDialog loadingDialog;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    DataControlTower dt;

    /** 이벤트 생성 중에는 난이도 버튼을 다시 눌러도 무시 (연타로 요청이 여러 번 나가는 것 방지) */
    private boolean isGenerating = false;
    private int successCount = 0;
    private int failCount = 0;

    private FrameLayout btnEasy, btnNormal, btnHard, btnNoAiEasy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_difficulty);
        dt = DataControlTower.getInstance(this);

        btnEasy = findViewById(R.id.btn_easy);
        btnNormal = findViewById(R.id.btn_normal);
        btnHard = findViewById(R.id.btn_hard);
        btnNoAiEasy = findViewById(R.id.btn_no_ai_easy);

        setSfx(btnEasy, btnNormal, btnHard);

        btnEasy.setOnClickListener(v -> onDifficultySelected("EASY"));
        btnNormal.setOnClickListener(v -> onDifficultySelected("NORMAL"));
        btnHard.setOnClickListener(v -> onDifficultySelected("HARD"));
        btnNoAiEasy.setOnClickListener(v -> {
            if (isGenerating) return;
            isGenerating = true;
            setButtonsEnabled(false);
            dt.setDifficulty("EASY");
            startGame();
        });
    }

    private void onDifficultySelected(String difficulty) {
        if (isGenerating) return;   // 이미 생성 중이면 무시
        isGenerating = true;
        setButtonsEnabled(false);

        dt.setDifficulty(difficulty);
        generateEventsAndStart(dt.getDifficulty().eventCount);
    }

    private void setButtonsEnabled(boolean enabled) {
        for (FrameLayout b : new FrameLayout[]{btnEasy, btnNormal, btnHard, btnNoAiEasy}) {
            if (b != null) b.setEnabled(enabled);
        }
    }

    private void generateEventsAndStart(int targetCount) {
        successCount = 0;
        failCount = 0;

        loadingDialog = new AlertDialog.Builder(this)
                .setTitle("신들의 개입")
                .setMessage(progressMessage(targetCount))
                .setCancelable(false)
                .create();
        loadingDialog.show();

        List<AiType> shuffledGods = new ArrayList<>(Arrays.asList(AiType.values()));
        Collections.shuffle(shuffledGods);
        // 1층, 21층, 41층에 생성된 이벤트 출현
        // 요청은 동시에 보낸다 (중복 요청은 isGenerating으로 막으므로 고정 대기가 필요 없음)
        for (int i = 0; i < targetCount; i++) {
            AiType randomGod = shuffledGods.get(i % shuffledGods.size());
            int targetFloor = (i * 20) + 1;
            String eventType = "normal";
            if (randomGod == AiType.STRUGGLE) eventType = "battle";
            // 보물의 신: 상점 대신 심층 보물 중 하나를 골라 받는 일반 이벤트
            else if (randomGod == AiType.TREASURE) eventType = "normal";

            generateWithRetry(targetFloor, randomGod, eventType, targetCount, 0);
        }
    }

    private void generateWithRetry(int targetFloor, AiType randomGod, String eventType,
                                   int targetCount, int retryCount) {
        dt.getAiManager().generate(
                targetFloor,
                dt.getPlayer().getStat(),
                // 보물의 신에게는 심층 장비만 보여준다 (그 안에서 고르도록)
                randomGod == AiType.TREASURE ? dt.getDeepFloorGear() : dt.getItemManager().getAll(),
                dt.getMonstersForFloor(targetFloor, targetFloor + 10), // 이 층대에 실제로 나오는 몬스터만
                eventType,
                randomGod,
                new AiCallback() {
                    @Override
                    public void onSuccess(GameEvent newEvent) {
                        runOnUiThread(() -> {
                            dt.addAiEvent(targetFloor, newEvent);
                            successCount++;
                            checkProgress(targetCount);
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "실패 (시도 " + (retryCount + 1) + "): " + errorMessage);
                        if (retryCount < MAX_RETRY) {
                            // 실패했을 때만 짧게 기다렸다가 재시도 (429 레이트 리밋 대비)
                            mainHandler.postDelayed(() ->
                                            generateWithRetry(targetFloor, randomGod, eventType,
                                                    targetCount, retryCount + 1),
                                    RETRY_DELAY_MS * (retryCount + 1)
                            );
                        } else {
                            runOnUiThread(() -> {
                                failCount++;
                                checkProgress(targetCount);
                            });
                        }
                    }
                }
        );
    }

    private String progressMessage(int total) {
        String msg = "운명을 창조하는 중입니다...\n(" + successCount + " / " + total + ")";
        if (failCount > 0) msg += " 실패 " + failCount;
        return msg + "\n잠시 기다려 주십시오...";
    }

    private void checkProgress(int total) {
        if (isFinishing() || isDestroyed()) return;
        if (loadingDialog == null || !loadingDialog.isShowing()) return;

        loadingDialog.setMessage(progressMessage(total));
        if (successCount + failCount >= total) {
            loadingDialog.dismiss();
            startGame();
        }
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacksAndMessages(null);
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
        super.onDestroy();
    }

    private void startGame() {
        Intent intent = new Intent(this, EventActivity.class);
        startActivity(intent);
        finish();
    }
}