package com.textdungeon.ai.backend;

import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 감싼 콜백을 한 번만 부른다. 타임아웃과 늦게 도착한 응답이 겹쳐도 결과가 두 번 전달되지 않게 한다.
 * 두 번째부터는 로그만 남기고 버린다.
 */
public final class OnceCallback implements LlmCallback {
    private static final String TAG = "OnceCallback";

    private final LlmCallback delegate;
    private final AtomicBoolean called = new AtomicBoolean(false);

    public OnceCallback(LlmCallback delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onResult(String text) {
        if (called.compareAndSet(false, true)) delegate.onResult(text);
        else Log.w(TAG, "이미 결과를 전달해서 응답을 버림");
    }

    @Override
    public void onError(String errorMessage) {
        if (called.compareAndSet(false, true)) delegate.onError(errorMessage);
        else Log.w(TAG, "이미 결과를 전달해서 오류를 버림: " + errorMessage);
    }
}
