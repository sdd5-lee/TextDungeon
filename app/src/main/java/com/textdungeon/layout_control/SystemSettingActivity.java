package com.textdungeon.layout_control;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import com.example.textdungeon.R;

public class SystemSettingActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_setting);

        SeekBar sbVolume = findViewById(R.id.sb_volume);
        RadioGroup rgFontSize = findViewById(R.id.rg_font_size);
        FrameLayout btnBack = findViewById(R.id.btn_back);

        sbVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // TODO: Volume Control
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}