package com.textdungeon.activity_control;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.widget.SwitchCompat;
import com.example.textdungeon.R;
import com.textdungeon.ai.AiManager;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.system.SoundManager;

public class SystemSettingActivity extends BaseActivity {

    private SharedPreferences prefs;
    private TextView tvOnDeviceStatus;
    private final Handler statusHandler = new Handler(Looper.getMainLooper());
    // 화면이 떠 있는 동안 1초마다 온디바이스 AI 상태(받는 중 %)를 새로 고친다
    private final Runnable statusUpdater = new Runnable() {
        @Override
        public void run() {
            tvOnDeviceStatus.setText(DataControlTower.getInstance(SystemSettingActivity.this)
                    .getAiManager().getOnDeviceStatusLine());
            statusHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_setting);

        prefs = getSharedPreferences("GameSettings", Context.MODE_PRIVATE);

        SeekBar sbVolume = findViewById(R.id.sb_volume);
        RadioGroup rgFontSize = findViewById(R.id.rg_font_size);
        FrameLayout btnBack = findViewById(R.id.btn_back);
        FrameLayout btnOpenSource = findViewById(R.id.btn_opensource);
        tvOnDeviceStatus = findViewById(R.id.tv_ondevice_status);
        setupModelSwitch(findViewById(R.id.sw_ondevice_model));

        // 모든 버튼에 효과음 일괄 적용
        setSfx(btnBack, btnOpenSource);

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

        btnOpenSource.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("오픈소스 라이선스")
                    .setMessage("본 앱은 다음의 오픈소스 라이브러리를 사용하고 있습니다.\n\n" +
                            "■ Apache License 2.0\n" +
                            "- AndroidX & Material Components\n" +
                            "- Google Gson\n" +
                            "- AndroidX Media3 (ExoPlayer)\n" +
                            "- OkHttp\n" +
                            "- LiteRT-LM\n" +
                            "- Gemma 4 E2B 모델 (혼돈의 주사위)\n" +
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

        btnBack.setOnClickListener(v -> finish());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    /** 켜면 Wi-Fi에서 받기 시작, 끄면 받던 것을 취소하고 받아 둔 모델을 지운다 (지울 게 있으면 먼저 묻는다) */
    private void setupModelSwitch(SwitchCompat swModel) {
        AiManager ai = DataControlTower.getInstance(this).getAiManager();
        swModel.setChecked(ai.isModelDownloadEnabled());
        // 모델을 못 쓰는 기기는 받지도 않으므로 스위치를 막는다 (상태 줄에 이유가 나온다)
        swModel.setEnabled(ai.isOnDeviceSupported());
        swModel.setOnCheckedChangeListener((button, on) -> {
            if (on == ai.isModelDownloadEnabled()) return;
            if (on || !ai.hasOnDeviceModel()) {
                ai.setModelDownloadEnabled(on);
                return;
            }
            new android.app.AlertDialog.Builder(this)
                    .setTitle("AI 모델 지우기")
                    .setMessage("받아 둔 모델(2.6GB)을 지웁니다. 혼돈의 주사위는 클라우드로만 동작합니다.\n다시 켜면 Wi-Fi에서 새로 받습니다.")
                    .setPositiveButton("지우기", (dialog, which) -> ai.setModelDownloadEnabled(false))
                    .setNegativeButton("취소", (dialog, which) -> button.setChecked(true))
                    .setOnCancelListener(dialog -> button.setChecked(true))
                    .show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        statusHandler.post(statusUpdater);
    }

    @Override
    protected void onPause() {
        super.onPause();
        statusHandler.removeCallbacks(statusUpdater);
    }
}