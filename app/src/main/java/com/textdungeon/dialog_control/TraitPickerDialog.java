package com.textdungeon.dialog_control;

import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.textdungeon.R;
import com.textdungeon.model.Job;
import com.textdungeon.model.Trait;
import com.textdungeon.system.SoundManager;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 캐릭터 선택에서 "특성 바꾸기"를 누르면 뜨는 특성 선택 창. 전투 창의 가방처럼 특성 버튼을 두 줄로 깔고 처음에는 목록만 보여 준다.
 * 버튼을 누르면 아래로 설명 칸이 펼쳐지고, "이 특성으로"를 눌러야 바뀐다.
 */
public class TraitPickerDialog extends Dialog {

    public interface Listener {
        void onTraitChosen(Trait trait);
    }

    private static final long EXPAND_MS = 240;

    private final Job job;
    private final List<Trait> traits;
    private final Trait current;
    private final Listener listener;
    private final Map<Trait, TextView> buttons = new EnumMap<>(Trait.class);
    /** 설명 칸에 보이는 (금색 테두리) 특성. 아직 아무것도 안 눌렀으면 null */
    private Trait shown;
    private View overlay, panel, descScroll;
    private TextView tvName, tvTag, tvDescription, btnOk;

    public TraitPickerDialog(Context context, Job job, List<Trait> traits, Trait current, Listener listener) {
        super(context);
        this.job = job;
        this.traits = traits;
        this.current = current;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_trait_picker);
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // 창은 화면 전체라서 판 바깥(빈 곳)을 누르면 닫는다
        overlay = findViewById(R.id.trait_overlay);
        overlay.setOnClickListener(v -> cancel());
        panel = findViewById(R.id.trait_root);
        DisplayMetrics screen = getContext().getResources().getDisplayMetrics();
        panel.getLayoutParams().width = (int) (screen.widthPixels * 0.92);
        // 버튼 판은 화면 높이의 42%로 크게 고정. 특성이 늘어 넘치면 판 안에서 스크롤된다
        findViewById(R.id.trait_grid_scroll).getLayoutParams().height = (int) (screen.heightPixels * 0.42);

        tvName = findViewById(R.id.tv_pick_name);
        tvTag = findViewById(R.id.tv_pick_tag);
        tvDescription = findViewById(R.id.tv_pick_description);
        descScroll = findViewById(R.id.trait_desc_scroll);
        btnOk = findViewById(R.id.btn_trait_ok);

        GridLayout grid = findViewById(R.id.trait_grid);
        for (Trait t : traits) grid.addView(button(t));

        findViewById(R.id.btn_trait_close).setOnClickListener(v -> {
            playSfx();
            dismiss();
        });
        btnOk.setOnClickListener(v -> {
            playSfx();
            dismiss();
            listener.onTraitChosen(shown);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Window window = getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            // 화면 전체 창이면 내비게이션 막대가 다시 나오므로 게임 화면처럼 숨긴다
            WindowInsetsControllerCompat bars = WindowCompat.getInsetsController(window, window.getDecorView());
            bars.hide(WindowInsetsCompat.Type.systemBars());
            bars.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

    /** 특성 버튼 하나 (전투 가방 버튼 모양). 지금 쓰는 특성은 이름 앞에 ✓ */
    private TextView button(Trait t) {
        TextView b = new TextView(getContext());
        b.setText(t == current ? "✓ " + t.displayName : t.displayName);
        b.setTextSize(14);
        b.setTextColor(0xFFE9C176);
        b.setTypeface(Typeface.SERIF, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setSingleLine();
        b.setEllipsize(TextUtils.TruncateAt.END);
        b.setPadding(dp(6), 0, dp(6), 0);
        b.setBackgroundResource(R.drawable.bg_trait_btn);
        b.setOnClickListener(v -> {
            playSfx();
            show(t);
        });
        buttons.put(t, b);

        // 두 칸에 같은 너비로 나눠 채운다
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f));
        lp.width = 0;
        lp.height = dp(48);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        b.setLayoutParams(lp);
        return b;
    }

    /** 누른 특성을 금색 테두리로 표시하고 설명 칸에 보여 준다 (처음 누를 때 칸을 펼친다). 이미 쓰는 특성이면 "이 특성으로"를 막는다 */
    private void show(Trait t) {
        if (shown == null) expandDescription();
        shown = t;
        for (Map.Entry<Trait, TextView> e : buttons.entrySet()) {
            e.getValue().setSelected(e.getKey() == t);
        }
        List<String> tags = new ArrayList<>();
        if (t == job.trait) tags.add("기본 특성");
        if (t == current) tags.add("사용 중");
        tvName.setText(t.displayName);
        tvTag.setText(TextUtils.join(" · ", tags));
        tvTag.setVisibility(tags.isEmpty() ? View.GONE : View.VISIBLE);
        tvDescription.setText(t.description);
        descScroll.scrollTo(0, 0);

        boolean inUse = t == current;
        btnOk.setText(inUse ? "사용 중" : "이 특성으로");
        btnOk.setEnabled(!inUse);
        btnOk.setAlpha(inUse ? 0.45f : 1f);
    }

    /** 설명 칸을 0에서 제 높이까지 펼친다. 판의 윗변은 그대로 두고 아래로 늘어나되, 화면 아래를 넘으면 그만큼 올라간다 */
    private void expandDescription() {
        FrameLayout.LayoutParams panelLp = (FrameLayout.LayoutParams) panel.getLayoutParams();
        ViewGroup.MarginLayoutParams descLp = (ViewGroup.MarginLayoutParams) descScroll.getLayoutParams();
        int height = descLp.height;
        int margin = descLp.topMargin;
        int startTop = panel.getTop();
        int roomBelow = overlay.getHeight() - dp(16) - (panel.getHeight() + height + margin);
        int endTop = Math.max(dp(16), Math.min(startTop, roomBelow));

        panelLp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        panelLp.topMargin = startTop;
        descLp.height = 0;
        descLp.topMargin = 0;
        descScroll.setVisibility(View.VISIBLE);
        btnOk.setVisibility(View.VISIBLE);

        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(EXPAND_MS);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            descLp.height = Math.round(height * f);
            descLp.topMargin = Math.round(margin * f);
            panelLp.topMargin = Math.round(startTop + (endTop - startTop) * f);
            panel.requestLayout();
        });
        anim.start();
    }

    private void playSfx() {
        SoundManager.getInstance(getContext()).playButtonSfx();
    }

    private int dp(float v) {
        return Math.round(v * getContext().getResources().getDisplayMetrics().density);
    }
}
