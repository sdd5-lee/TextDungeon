package com.textdungeon.dialog_control;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.example.textdungeon.R;
import com.google.android.material.snackbar.Snackbar;
import com.textdungeon.activity_control.BaseActivity;
import com.textdungeon.data.DataControl;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.model.Achievement;
import com.textdungeon.model.Magic;
import com.textdungeon.model.MagicSchool;
import com.textdungeon.player.MagicScroll;
import com.textdungeon.player.Player;
import com.textdungeon.system.SoundManager;

import java.util.List;

/**
 * 마법서. 마법 나무에서 마법을 누르면 아래에 조건(계열·이전 마법·지혜)과 버튼이 나온다.
 * 계열은 코어 포인트로 열고, 계열 안에서는 앞 마법을 배운 뒤 지혜 조건을 채우면 배운다.
 */
public class MagicLearnDialog extends Dialog {

    private static final int OK_COLOR = 0xFF2E6B2A;
    private static final int NO_COLOR = 0xFF9A2A22;
    /** 나무 모양: 가운데에서 퍼지는 마법진, 아래에서 자라는 세피로트의 나무 */
    private static final MagicTreeView.Shape SHAPE = MagicTreeView.Shape.CIRCLE;

    private final Player player;
    private final DataControl<Magic> magicManager;

    private MagicTreeView tree;
    private TextView tvWisdom, tvCoreInfo;
    private TextView tvName, tvSchool, tvConditions, btnAction;
    private Magic selected;

    public MagicLearnDialog(Context context, Player player, DataControl<Magic> magicManager) {
        super(context);
        this.player = player;
        this.magicManager = magicManager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_magic_tree);
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        tvWisdom = findViewById(R.id.tv_current_wisdom);
        tvCoreInfo = findViewById(R.id.tv_core_info);
        tvName = findViewById(R.id.tv_detail_name);
        tvSchool = findViewById(R.id.tv_detail_school);
        tvConditions = findViewById(R.id.tv_detail_conditions);
        btnAction = findViewById(R.id.btn_detail_action);
        findViewById(R.id.btn_magic_close).setOnClickListener(v -> {
            SoundManager.getInstance(getContext()).playButtonSfx();
            dismiss();
        });

        boolean rising = SHAPE == MagicTreeView.Shape.RISING;
        ((TextView) findViewById(R.id.tv_magic_title)).setText(rising ? "세피로트의 나무" : "마법진");
        tvSchool.setText(rising
                ? "아래 기초 마법에서 계열마다 가지가 위로 자랍니다."
                : "가운데 기초 마법에서 계열마다 갈래가 뻗어 나갑니다.");

        tree = findViewById(R.id.magic_tree);
        tree.setShape(SHAPE);
        tree.setMagics(magicManager.getAll(), player.getMagicScroll(), this::showDetail);
        refresh();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Window window = getWindow();
        if (window != null) {
            int width = (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.95);
            int height = (int) (getContext().getResources().getDisplayMetrics().heightPixels * 0.90);
            window.setLayout(width, height);
        }
    }

    private int wisdom() {
        return player.getStat().getWisdom();
    }

    private void refresh() {
        MagicScroll scroll = player.getMagicScroll();
        int wis = wisdom();
        tvWisdom.setText("지혜 " + wis);
        tvCoreInfo.setText("코어 " + scroll.getAvailableCores(wis)
                + "개 · 다음은 지혜 " + scroll.getNextCoreWisdom(wis));
        tree.refresh(wis);
        if (selected != null) showDetail(selected);
    }

    private void showDetail(Magic magic) {
        selected = magic;
        MagicScroll scroll = player.getMagicScroll();
        int wis = wisdom();
        MagicSchool school = magic.getSchool();
        MagicScroll.LearnState state = scroll.getLearnState(magic, wis);

        tvName.setText(magic.getName());
        tvSchool.setText(school.needsCore()
                ? school.displayName + " 계열 · " + tierOf(magic) + "단계"
                : "기초 마법 · 코어 없이 배운다");

        SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append("기본 피해 ").append(String.valueOf(magic.getBaseDamage())).append(" (지혜 2마다 +1)");
        if (school.needsCore()) {
            boolean open = scroll.isSchoolOpen(school);
            line(sb, open, open
                    ? school.displayName + " 계열 열림"
                    : school.displayName + " 계열이 닫혀 있습니다 (코어 1개로 연다)");
        }
        if (magic.getRequires() != null) {
            Magic prev = magicManager.spawn(magic.getRequires());
            String prevName = prev != null ? prev.getName() : magic.getRequires();
            boolean learned = scroll.hasMagic(magic.getRequires());
            line(sb, learned, learned
                    ? "이전 마법: " + prevName
                    : "이전 마법(" + prevName + ")을 배워야 해금할 수 있습니다");
        }
        line(sb, wis >= magic.getValue(), "요구 지혜 " + magic.getValue() + " (현재 " + wis + ")");
        tvConditions.setText(sb);

        btnAction.setVisibility(View.VISIBLE);
        btnAction.setOnClickListener(null);
        switch (state) {
            case LEARNED:
                setAction("배운 마법", R.drawable.bg_magic_disabled, 0xFFD8C6A4);
                break;
            case CAN_LEARN:
                setAction("배우기", R.drawable.bg_magic_learn, 0xFFB9E3B3);
                btnAction.setOnClickListener(v -> learn(magic));
                break;
            case SCHOOL_LOCKED:
                if (scroll.getAvailableCores(wis) > 0) {
                    setAction(school.displayName + " 계열 열기 (코어 1)", R.drawable.bg_magic_open, 0xFFF0CF86);
                    btnAction.setOnClickListener(v -> openSchool(school));
                } else {
                    setAction("코어 포인트가 없습니다 (다음: 지혜 " + scroll.getNextCoreWisdom(wis) + ")",
                            R.drawable.bg_magic_disabled, 0xFFD8C6A4);
                }
                break;
            case NEED_PREVIOUS:
                setAction("이전 마법을 먼저 배우세요", R.drawable.bg_magic_disabled, 0xFFD8C6A4);
                break;
            case NEED_WISDOM:
                setAction("지혜가 부족합니다", R.drawable.bg_magic_disabled, 0xFFD8C6A4);
                break;
        }
    }

    private static void line(SpannableStringBuilder sb, boolean ok, String text) {
        sb.append('\n');
        int start = sb.length();
        sb.append(ok ? "✔ " : "✖ ");
        sb.setSpan(new ForegroundColorSpan(ok ? OK_COLOR : NO_COLOR), start, sb.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append(text);
    }

    private void setAction(String text, int background, int textColor) {
        btnAction.setText(text);
        btnAction.setBackgroundResource(background);
        btnAction.setTextColor(textColor);
    }

    /** 계열 안에서 몇 번째 마법인지 (첫 마법이 1단계) */
    private int tierOf(Magic magic) {
        int tier = 1;
        Magic cur = magic;
        while (cur.getRequires() != null && tier < 10) {
            Magic prev = magicManager.spawn(cur.getRequires());
            if (prev == null || prev.getSchool() != magic.getSchool()) break;
            tier++;
            cur = prev;
        }
        return tier;
    }

    private void openSchool(MagicSchool school) {
        SoundManager.getInstance(getContext()).playButtonSfx();
        if (player.getMagicScroll().openSchool(school, wisdom())) {
            showBar(school.displayName + " 계열을 열었습니다!");
        }
        refresh();
    }

    private void learn(Magic magic) {
        SoundManager.getInstance(getContext()).playButtonSfx();
        player.getMagicScroll().addMagic(magic.getId(), magic.getMaxCount());
        showBar(magic.getName() + " 마법의 이치를 깨달았습니다!");
        List<Achievement> unlocked = DataControlTower.getInstance(getContext())
                .getAchievementManager().updateProgress("magic_learn", 1, true);
        // getContext()는 테마로 감싼 것이라 Activity가 아니다. 창을 띄운 Activity는 getOwnerActivity()
        if (getOwnerActivity() instanceof BaseActivity) {
            ((BaseActivity) getOwnerActivity()).showAchievementNotification(unlocked);
        }
        refresh();
    }

    /** 마법서 창 위에 띄운다 (창을 닫지 않으므로 화면 뒤에 띄우면 안 보인다) */
    private void showBar(String message) {
        View root = findViewById(android.R.id.content);
        if (root == null) return;
        Snackbar snackbar = Snackbar.make(root, message, Snackbar.LENGTH_SHORT);
        snackbar.setBackgroundTint(Color.parseColor("#901418"));
        snackbar.setTextColor(Color.parseColor("#E5E2E1"));
        snackbar.show();
    }
}
