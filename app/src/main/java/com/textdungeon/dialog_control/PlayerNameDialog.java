package com.textdungeon.dialog_control;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.textdungeon.R;
import com.textdungeon.buttons.SingleClickListener;
import com.textdungeon.model.AdventurerNames;
import com.textdungeon.model.Job;
import com.textdungeon.model.Trait;
import com.textdungeon.system.SoundManager;

import java.util.List;
import java.util.Random;

/**
 * 캐릭터 선택에서 시작을 누르면 뜨는 "당신의 이름은?" 창.
 * 처음부터 주사위 이름이 들어 있고, 주사위·지난번 이름으로 바꾸거나 직접 쓴다. 비우고 출발하면 주사위 이름.
 */
public class PlayerNameDialog extends Dialog {

    public interface Listener {
        void onNameChosen(String name);
    }

    private final Job job;
    private final Trait trait;
    private final List<String> recentNames;
    private final Listener listener;
    private final Random rnd = new Random();
    private EditText editName;

    public PlayerNameDialog(Context context, Job job, Trait trait, List<String> recentNames, Listener listener) {
        super(context);
        this.job = job;
        this.trait = trait;
        this.recentNames = recentNames;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_player_name);
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // 양피지 그림은 네모라서 모서리를 둥글게 잘라 낸다
        View root = findViewById(R.id.name_root);
        float radius = dp(12);
        root.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        root.setClipToOutline(true);

        ((TextView) findViewById(R.id.tv_name_job)).setText(job.name + " · " + trait.displayName);

        editName = findViewById(R.id.edit_player_name);
        editName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(AdventurerNames.MAX_LENGTH)});
        setName(AdventurerNames.random(job, rnd));
        editName.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) return false;
            confirm();
            return true;
        });

        findViewById(R.id.btn_name_dice).setOnClickListener(v -> {
            playSfx();
            setName(AdventurerNames.random(job, rnd));
        });
        findViewById(R.id.btn_name_back).setOnClickListener(v -> {
            playSfx();
            dismiss();
        });
        findViewById(R.id.btn_name_ok).setOnClickListener(SingleClickListener.wrap(v -> {
            playSfx();
            confirm();
        }));
        showRecentNames();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Window window = getWindow();
        if (window != null) {
            int width = (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.88);
            window.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void showRecentNames() {
        if (recentNames.isEmpty()) {
            findViewById(R.id.tv_recent_label).setVisibility(View.GONE);
            findViewById(R.id.recent_scroll).setVisibility(View.GONE);
            return;
        }
        LinearLayout row = findViewById(R.id.recent_names);
        for (String name : recentNames) {
            TextView chip = new TextView(getContext());
            chip.setText(name);
            chip.setTextSize(13);
            chip.setTypeface(Typeface.SERIF);
            chip.setTextColor(0xFF4A2E1A);
            chip.setBackgroundResource(R.drawable.bg_recent_name);
            chip.setPadding(dp(11), dp(5), dp(11), dp(5));
            chip.setOnClickListener(v -> {
                playSfx();
                setName(name);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(6));
            row.addView(chip, lp);
        }
    }

    private void setName(String name) {
        editName.setText(name);
        editName.setSelection(editName.length());
    }

    private void confirm() {
        String name = editName.getText().toString().trim();
        if (name.isEmpty()) name = AdventurerNames.random(job, rnd);
        dismiss();
        listener.onNameChosen(name);
    }

    private void playSfx() {
        SoundManager.getInstance(getContext()).playButtonSfx();
    }

    private int dp(float v) {
        return Math.round(v * getContext().getResources().getDisplayMetrics().density);
    }
}
