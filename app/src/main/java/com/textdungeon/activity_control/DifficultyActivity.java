package com.textdungeon.activity_control;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.example.textdungeon.R;
import com.textdungeon.ai.AiCallback;
import com.textdungeon.ai.AiManager;
import com.textdungeon.ai.AiType;
import com.textdungeon.ai.backend.OnDeviceBackend;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.event.GameEvent;
import com.textdungeon.model.Reward;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DifficultyActivity extends BaseActivity {
    private static final String TAG = "DifficultyActivity";
    private static final int MAX_RETRY = 1;
    /** 모델이 올라오길 기다리는 최대 시간. S24 GPU는 4초 안팎 (0-B) */
    private static final long MODEL_WAIT_MS = 20_000L;
    private static final long MODEL_POLL_MS = 200L;
    /** 이벤트 하나를 기다리는 최대 시간. S24에서 틀 방식은 5초 안팎. 넘기면 남은 이벤트 없이 시작한다 */
    private static final long EVENT_TIMEOUT_MS = 30_000L;

    private AlertDialog loadingDialog;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    DataControlTower dt;

    /** 이벤트 생성 중에는 난이도 버튼을 다시 눌러도 무시 (연타로 요청이 여러 번 나가는 것 방지) */
    private boolean isGenerating = false;
    private int successCount = 0;
    private int failCount = 0;
    /** 기다리다 포기한 요청의 콜백이 늦게 와도 버리도록 요청마다 올린다 */
    private int requestSeq = 0;
    private List<AiType> gods;

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
            dt.getAiManager().warmUpOnDevice(); // 주사위용 모델을 뒤에서 올린다
            dt.setDifficulty("EASY");
            startGame();
        });
    }

    private void onDifficultySelected(String difficulty) {
        if (isGenerating) return;   // 이미 생성 중이면 무시
        isGenerating = true;
        setButtonsEnabled(false);
        // 이벤트와 주사위가 같은 온디바이스 모델을 쓴다. 올라오면 이벤트부터 만든다
        dt.getAiManager().warmUpOnDevice();

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
        gods = new ArrayList<>(Arrays.asList(AiType.values()));
        Collections.shuffle(gods);

        loadingDialog = new AlertDialog.Builder(this)
                .setTitle("신들의 개입")
                .setMessage("신들을 깨우는 중입니다...\n잠시 기다려 주십시오...")
                .setCancelable(false)
                .create();
        loadingDialog.show();
        waitForModel(targetCount, SystemClock.uptimeMillis() + MODEL_WAIT_MS);
    }

    /** 모델 로딩이 끝나면(준비됨, 실패, 모델 없음) 이벤트를 만든다. 쓸 수 없으면 AI 이벤트 없이 시작한다 */
    private void waitForModel(int targetCount, long deadline) {
        AiManager ai = dt.getAiManager();
        if (ai.getOnDeviceState() == OnDeviceBackend.State.LOADING && SystemClock.uptimeMillis() < deadline) {
            mainHandler.postDelayed(() -> waitForModel(targetCount, deadline), MODEL_POLL_MS);
            return;
        }
        if (!ai.isOnDeviceReady()) {
            Log.w(TAG, "온디바이스 모델을 쓸 수 없어 AI 이벤트 없이 시작 (" + ai.getOnDeviceState() + ")");
            Toast.makeText(this, "신들의 이벤트 없이 시작합니다\n" + ai.getOnDeviceStatusLine(), Toast.LENGTH_LONG).show();
            finishGeneration();
            return;
        }
        loadingDialog.setMessage(progressMessage(targetCount));
        generateNext(0, targetCount, 0);
    }

    /**
     * 이벤트를 하나씩 차례로 만든다 (온디바이스 모델은 한 번에 요청 하나만 받는다).
     * 1층, 21층, 41층에 생성된 이벤트 출현
     */
    private void generateNext(int index, int targetCount, int retryCount) {
        if (index >= targetCount) {
            finishGeneration();
            return;
        }
        AiType god = gods.get(index % gods.size());
        int targetFloor = (index * 20) + 1;
        int seq = ++requestSeq;
        Runnable timeout = () -> {
            if (seq != requestSeq) return;
            requestSeq++;
            Log.w(TAG, targetFloor + "층 이벤트가 " + EVENT_TIMEOUT_MS + "ms 안에 오지 않아 남은 이벤트 없이 시작");
            dt.getAiManager().cancelOnDevice();
            failCount += targetCount - index;
            finishGeneration();
        };
        mainHandler.postDelayed(timeout, EVENT_TIMEOUT_MS);

        dt.getAiManager().generateEvent(
                targetFloor,
                god,
                // 형상 등급별 아이템 후보: 하급은 이 층대, 중급은 더 깊은 층 장비, 상급은 심층 장비
                dt.getItemsForFloor(targetFloor - 5, targetFloor + 5),
                dt.getUpperGear(targetFloor),
                dt.getDeepFloorGear(),
                dt.getMonstersForFloor(targetFloor, targetFloor + 10), // 이 층대에 실제로 나오는 몬스터만
                dt.getDifficulty().rewardMultiplier,
                Reward.expMultiplier(dt.getPlayer()),
                new AiCallback() {
                    @Override
                    public void onSuccess(GameEvent newEvent) {
                        runOnUiThread(() -> {
                            if (seq != requestSeq) return;
                            mainHandler.removeCallbacks(timeout);
                            dt.addAiEvent(targetFloor, newEvent);
                            successCount++;
                            updateProgress(targetCount);
                            generateNext(index + 1, targetCount, 0);
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(() -> {
                            if (seq != requestSeq) return;
                            mainHandler.removeCallbacks(timeout);
                            Log.e(TAG, targetFloor + "층 실패 (시도 " + (retryCount + 1) + "): " + errorMessage);
                            if (retryCount < MAX_RETRY) {
                                generateNext(index, targetCount, retryCount + 1);
                                return;
                            }
                            failCount++;
                            updateProgress(targetCount);
                            generateNext(index + 1, targetCount, 0);
                        });
                    }
                }
        );
    }

    private String progressMessage(int total) {
        String msg = "운명을 창조하는 중입니다...\n(" + successCount + " / " + total + ")";
        if (failCount > 0) msg += " 실패 " + failCount;
        return msg + "\n잠시 기다려 주십시오...";
    }

    private void updateProgress(int total) {
        if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.setMessage(progressMessage(total));
    }

    private void finishGeneration() {
        if (isFinishing() || isDestroyed()) return;
        if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
        startGame();
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacksAndMessages(null);
        requestSeq++; // 화면을 떠난 뒤 오는 콜백은 버린다
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
