package com.textdungeon.activity_control;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.textdungeon.system.SoundManager;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullScreenMode();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("GameSettings", Context.MODE_PRIVATE);
        float fontScale = prefs.getFloat("fontScale", 1.0f);

        Configuration config = new Configuration(newBase.getResources().getConfiguration());
        config.fontScale = fontScale;

        Context context = newBase.createConfigurationContext(config);
        super.attachBaseContext(context);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences("GameSettings", Context.MODE_PRIVATE);
        float volumeFloat = prefs.getInt("volume", 50) / 100f;

        SoundManager sm = SoundManager.getInstance(this);
        sm.setBgmVolume(volumeFloat);
        sm.setSfxVolume(volumeFloat);

        sm.playBgm();
    }

    @Override
    protected void onPause() {
        super.onPause();
        SoundManager.getInstance(this).pauseBgm();
    }

    protected void setSfx(View... views) {
        for (View v : views) {
            if (v != null) {
                v.setOnTouchListener((view, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        SoundManager.getInstance(this).playButtonSfx();
                    }
                    return false;
                });
            }
        }
    }

    private void setFullScreenMode() {
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (windowInsetsController != null) {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
            windowInsetsController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }
}