package com.textdungeon.ai;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.textdungeon.ai.backend.OnDeviceDebugFlags;

/**
 * 온디바이스 디버그 플래그를 바꾸고 바로 닫힌다 (디버그 빌드 전용, 화면 없음).
 *
 *   adb shell am start -n com.lkj.textdungeon/com.textdungeon.ai.OnDeviceFlagsActivity
 *     --es backend cpu     gpu(기본) / auto(GPU가 실패하면 CPU) / cpu(에뮬레이터)
 *     --ez skipram true    램 기준 검사를 건너뜀 (에뮬레이터)
 *     --el delay 15000     온디바이스 응답을 일부러 늦춤 (ms)
 *     --el timeout 1500    온디바이스 타임아웃 덮어쓰기 (ms, 0이면 기본값)
 *     --ez reset true      모두 기본값으로
 *
 * 넘긴 값만 바뀐다. 장치와 램 검사는 다음 로드부터 적용되므로, 모델이 이미 올라갔거나 FAILED면 앱을 다시 시작한다.
 * 결과는 Logcat 태그 SPIKE.
 */
public class OnDeviceFlagsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        SharedPreferences prefs = getSharedPreferences(OnDeviceDebugFlags.PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (intent.getBooleanExtra("reset", false)) editor.clear();
        if (intent.hasExtra("backend")) editor.putString(OnDeviceDebugFlags.KEY_BACKEND, intent.getStringExtra("backend"));
        if (intent.hasExtra("skipram")) {
            editor.putBoolean(OnDeviceDebugFlags.KEY_SKIP_RAM_CHECK, intent.getBooleanExtra("skipram", false));
        }
        if (intent.hasExtra("delay")) editor.putLong(OnDeviceDebugFlags.KEY_DELAY_MS, intent.getLongExtra("delay", 0));
        if (intent.hasExtra("timeout")) editor.putLong(OnDeviceDebugFlags.KEY_TIMEOUT_MS, intent.getLongExtra("timeout", 0));
        editor.commit();

        String summary = "온디바이스 플래그: backend=" + OnDeviceDebugFlags.backend(this)
                + ", skipram=" + OnDeviceDebugFlags.skipRamCheck(this)
                + ", delay=" + OnDeviceDebugFlags.delayMs(this)
                + ", timeout=" + OnDeviceDebugFlags.timeoutMs(this, AiTimeouts.ON_DEVICE_MS);
        Log.i("SPIKE", summary);
        Toast.makeText(this, summary, Toast.LENGTH_LONG).show();
        finish();
    }
}
