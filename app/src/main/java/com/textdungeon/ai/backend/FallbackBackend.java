package com.textdungeon.ai.backend;

import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;

/**
 * 우선 백엔드를 먼저 쓰고, 준비가 안 됐거나 실패하면 예비 백엔드로 넘긴다.
 * 예: primary = 온디바이스, secondary = 클라우드
 * 온디바이스 모델을 붙여도 실패 시 게임이 멈추지 않게 하는 안전망.
 *
 * 예비 백엔드로 넘기는 경우: primary가 준비 안 됨 / 오류 / 응답이 검사(ResponseValidator)를 통과하지 못함 / 시간 초과.
 * 시간 초과면 primary의 추론을 멈추고, 늦게 도착한 primary 응답은 버린다. 바깥 콜백은 한 번만 불린다.
 */
public class FallbackBackend implements LlmBackend {
    private static final String TAG = "FallbackBackend";

    // 타임아웃 예약용. 요청이 몰리지 않으므로 앱 전체에서 데몬 스레드 하나면 된다
    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "fallback-timeout");
        t.setDaemon(true);
        return t;
    });

    private final LlmBackend primary;
    private final LlmBackend secondary;
    private final LongSupplier primaryTimeoutMs;
    // 마지막 요청의 취소 표시. 바깥(ChaosDice 전체 시간 초과)에서 cancel()로 멈춘 요청은 예비 백엔드로 넘기지 않는다
    private volatile AtomicBoolean lastCancelled = new AtomicBoolean(false);

    /** @param primaryTimeoutMs 요청마다 읽는다 (디버그 플래그로 바꿀 수 있게) */
    public FallbackBackend(LlmBackend primary, LlmBackend secondary, LongSupplier primaryTimeoutMs) {
        this.primary = primary;
        this.secondary = secondary;
        this.primaryTimeoutMs = primaryTimeoutMs;
    }

    @Override
    public void generate(String prompt, ResponseFormat format, LlmCallback callback) {
        generate(prompt, format, ResponseValidator.ANY, callback);
    }

    @Override
    public void generate(String prompt, ResponseFormat format, ResponseValidator validator, LlmCallback callback) {
        LlmCallback once = new OnceCallback(callback);
        AtomicBoolean cancelled = new AtomicBoolean(false);
        lastCancelled = cancelled;
        if (!primary.isReady()) {
            Log.d(TAG, primary.name() + " 준비 안 됨 → " + secondary.name());
            generateSecondary(prompt, format, validator, once);
            return;
        }

        // primary 결과와 타임아웃 중 먼저 온 쪽만 진행한다
        AtomicBoolean settled = new AtomicBoolean(false);
        long timeoutMs = primaryTimeoutMs.getAsLong();
        ScheduledFuture<?> timeout = TIMER.schedule(() -> {
            if (cancelled.get() || !settled.compareAndSet(false, true)) return;
            Log.w(TAG, primary.name() + " 시간 초과(" + timeoutMs + "ms) → 추론을 멈추고 " + secondary.name() + "로 재시도");
            primary.cancel();
            generateSecondary(prompt, format, validator, once);
        }, timeoutMs, TimeUnit.MILLISECONDS);

        primary.generate(prompt, format, validator, new LlmCallback() {
            @Override
            public void onResult(String text) {
                if (!settled.compareAndSet(false, true)) {
                    Log.w(TAG, primary.name() + " 응답이 시간 초과 뒤에 도착해서 버림");
                    return;
                }
                timeout.cancel(false);
                logAnswered(primary);
                once.onResult(text);
            }

            @Override
            public void onError(String errorMessage) {
                if (!settled.compareAndSet(false, true)) return; // 시간 초과로 이미 넘어감
                timeout.cancel(false);
                if (cancelled.get()) {
                    once.onError(errorMessage);
                    return;
                }
                Log.w(TAG, primary.name() + " 실패 → " + secondary.name() + "로 재시도: " + errorMessage);
                generateSecondary(prompt, format, validator, once);
            }
        });
    }

    private void generateSecondary(String prompt, ResponseFormat format, ResponseValidator validator, LlmCallback once) {
        secondary.generate(prompt, format, validator, new LlmCallback() {
            @Override
            public void onResult(String text) {
                logAnswered(secondary);
                once.onResult(text);
            }

            @Override
            public void onError(String errorMessage) {
                once.onError(errorMessage);
            }
        });
    }

    private static void logAnswered(LlmBackend backend) {
        Log.i(TAG, "응답한 백엔드: " + backend.name());
        GeminiBackend.logRaw("응답한 백엔드", backend.name());
    }

    @Override
    public void cancel() {
        lastCancelled.set(true);
        primary.cancel();
        secondary.cancel();
    }

    @Override
    public boolean isReady() {
        return primary.isReady() || secondary.isReady();
    }

    @Override
    public String name() {
        return primary.name() + " → " + secondary.name();
    }
}
