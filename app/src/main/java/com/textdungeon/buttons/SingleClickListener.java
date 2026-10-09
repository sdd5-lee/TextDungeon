package com.textdungeon.buttons;

import android.os.SystemClock;
import android.view.View;

/**
 * 연타 방지 클릭 리스너.
 * 마지막 클릭 후 일정 시간 안에 들어온 클릭은 무시한다.
 * 시간 기록을 static으로 공유하므로, 서로 다른 버튼을 동시에 누르는 경우(두 선택지를 동시에 탭)도 막힌다.
 *
 * 응답을 기다려야 하는 AI 요청에는 이것만으로 부족하다 → 호출부에서 "진행 중" 플래그로 따로 잠글 것.
 */
public abstract class SingleClickListener implements View.OnClickListener {
    private static final long DEFAULT_INTERVAL_MS = 600;
    private static long lastClickTime = 0;

    private final long intervalMs;

    protected SingleClickListener() {
        this(DEFAULT_INTERVAL_MS);
    }

    protected SingleClickListener(long intervalMs) {
        this.intervalMs = intervalMs;
    }

    @Override
    public final void onClick(View v) {
        long now = SystemClock.elapsedRealtime();
        if (now - lastClickTime < intervalMs) return;
        lastClickTime = now;
        onSingleClick(v);
    }

    public abstract void onSingleClick(View v);

    /** 람다로 쓰기 위한 헬퍼: button.setOnClickListener(SingleClickListener.wrap(v -> ...)) */
    public static View.OnClickListener wrap(View.OnClickListener listener) {
        return new SingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                listener.onClick(v);
            }
        };
    }
}
