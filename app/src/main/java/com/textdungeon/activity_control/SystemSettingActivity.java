package com.textdungeon.activity_control;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import androidx.activity.OnBackPressedCallback;
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
        FrameLayout adminButton = findViewById(R.id.btn_admin);
        FrameLayout btnOpenSource = findViewById(R.id.btn_opensource);

        // 모든 버튼에 효과음 일괄 적용
        setSfx(btnBack, adminButton, btnOpenSource);

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

        adminButton.setOnClickListener(view ->
                startActivity(new Intent(this, AdminActivity.class)));

        btnOpenSource.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("오픈소스 라이선스")
                    .setMessage("본 앱은 다음의 오픈소스 라이브러리를 사용하고 있습니다.\n\n" +
                            "■ Apache License 2.0\n" +
                            "- AndroidX & Material Components\n" +
                            "- Google Gson, Guava\n" +
                            "- AndroidX Media3 (ExoPlayer)\n" +
                            "- Google Generative AI SDK\n" +
                            "- RuntimeTypeAdapterFactory\n\n" +
                            "■ BSD / MIT / Apache 2.0 License\n" +
                            "- Glide (Bumptech)\n\n" +
                            "■ 사운드 에셋\n" +
                            "- 'Menu_Select_00.wav' by LittleRobotS... (Freesound.org)\n" +
                            "- License: CC BY 4.0\n\n" +
                            "위 라이브러리들은 각 라이선스 규약을 따르며, 본 프로그램의 창작 권리 범위에서 제외됩니다.")
                    .setPositiveButton("확인", null)
                    .show();
        });

        btnBack.setOnClickListener(v -> moveMain());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                moveMain();
            }
        });
    }

    private void moveMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}