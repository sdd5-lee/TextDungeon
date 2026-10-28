package com.textdungeon.ai.backend;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.textdungeon.BuildConfig;

/**
 * 온디바이스 모델 디버그 플래그. 디버그 빌드에서만 읽고, 출시 빌드는 항상 기본값을 쓴다.
 * 디버그 빌드의 OnDeviceFlagsActivity로 바꾼다 (adb).
 * 지연과 타임아웃은 다음 요청부터, 장치와 램 검사는 다음 로드부터 적용된다 (FAILED면 앱을 다시 시작).
 */
public final class OnDeviceDebugFlags {
    public static final String PREFS = "OnDeviceDebug";
    /** gpu(기본, 출시와 같음) / auto(GPU가 실패하면 CPU) / cpu(에뮬레이터: GPU 시도가 3분 넘게 걸린다) */
    public static final String KEY_BACKEND = "backend";
    /** 램 기준 검사를 건너뜀 (에뮬레이터) */
    public static final String KEY_SKIP_RAM_CHECK = "skip_ram_check";
    /** 온디바이스 응답을 일부러 늦춤 (ms). 타임아웃 시험용 */
    public static final String KEY_DELAY_MS = "delay_ms";
    /** 온디바이스 타임아웃 덮어쓰기 (ms, 0이면 기본값) */
    public static final String KEY_TIMEOUT_MS = "timeout_ms";

    private OnDeviceDebugFlags() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String backend(Context context) {
        return BuildConfig.DEBUG ? prefs(context).getString(KEY_BACKEND, "gpu") : "gpu";
    }

    public static boolean skipRamCheck(Context context) {
        return BuildConfig.DEBUG && prefs(context).getBoolean(KEY_SKIP_RAM_CHECK, false);
    }

    public static long delayMs(Context context) {
        return BuildConfig.DEBUG ? prefs(context).getLong(KEY_DELAY_MS, 0) : 0;
    }

    public static long timeoutMs(Context context, long defaultMs) {
        long v = BuildConfig.DEBUG ? prefs(context).getLong(KEY_TIMEOUT_MS, 0) : 0;
        return v > 0 ? v : defaultMs;
    }
}
