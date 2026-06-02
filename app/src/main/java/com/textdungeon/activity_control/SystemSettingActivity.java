package com.textdungeon.activity_control; // 패키지명 확인 요망

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import com.example.textdungeon.R;
import com.textdungeon.system.SoundManager;

public class SystemSettingActivity extends BaseActivity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_setting);

        prefs = getSharedPreferences("GameSettings", Context.MODE_PRIVATE);

        SeekBar sbVolume = findViewById(R.id.sb_volume);
        RadioGroup rgFontSize = findViewById(R.id.rg_font_size);
        FrameLayout btnBack = findViewById(R.id.btn_back);

        setSfx(btnBack);

        int savedVolume = prefs.getInt("volume", 50);
        sbVolume.setProgress(savedVolume);

        float savedFontScale = prefs.getFloat("fontScale", 1.0f);
        if (savedFontScale == 0.8f) {
            rgFontSize.check(R.id.rb_font_small);
        } else if (savedFontScale == 1.2f) {
            rgFontSize.check(R.id.rb_font_large);
        } else {
            rgFontSize.check(R.id.rb_font_medium);
        }

        // 볼륨 슬라이더 이벤트
        sbVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float volumeFloat = progress / 100f;
                SoundManager.getInstance(SystemSettingActivity.this).setBgmVolume(volumeFloat);
                SoundManager.getInstance(SystemSettingActivity.this).setSfxVolume(volumeFloat);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                prefs.edit().putInt("volume", seekBar.getProgress()).apply();
                SoundManager.getInstance(SystemSettingActivity.this).playButtonSfx();
            }
        });

        // 폰트 크기 라디오 버튼 이벤트
        rgFontSize.setOnCheckedChangeListener((group, checkedId) -> {
            float newScale = 1.0f;
            if (checkedId == R.id.rb_font_small) {
                newScale = 0.8f;
            } else if (checkedId == R.id.rb_font_large) {
                newScale = 1.2f;
            }
            float currentScale = prefs.getFloat("fontScale", 1.0f);
            if (currentScale != newScale) {
                prefs.edit().putFloat("fontScale", newScale).apply();
                recreate();
            }
        });

        btnBack.setOnClickListener(v -> finish());
    }
}