package com.textdungeon.ai.backend;

import android.util.Log;

/**
 * 우선 백엔드를 먼저 쓰고, 준비가 안 됐거나 실패하면 예비 백엔드로 넘긴다.
 * 예: primary = 온디바이스, secondary = 클라우드
 * 온디바이스 모델을 붙여도 실패 시 게임이 멈추지 않게 하는 안전망.
 */
public class FallbackBackend implements LlmBackend {
    private static final String TAG = "FallbackBackend";

    private final LlmBackend primary;
    private final LlmBackend secondary;

    public FallbackBackend(LlmBackend primary, LlmBackend secondary) {
        this.primary = primary;
        this.secondary = secondary;
    }

    @Override
    public void generate(String prompt, ResponseFormat format, LlmCallback callback) {
        if (!primary.isReady()) {
            secondary.generate(prompt, format, callback);
            return;
        }
        primary.generate(prompt, format, new LlmCallback() {
            @Override
            public void onResult(String text) {
                callback.onResult(text);
            }

            @Override
            public void onError(String errorMessage) {
                Log.w(TAG, primary.name() + " 실패 → " + secondary.name() + "로 재시도: " + errorMessage);
                secondary.generate(prompt, format, callback);
            }
        });
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
