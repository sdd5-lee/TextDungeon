package com.textdungeon.activity_control;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.textdungeon.R;
import com.google.android.material.snackbar.Snackbar;
import com.textdungeon.billing.Entitlements;
import com.textdungeon.buttons.SingleClickListener;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.dialog_control.PlayerNameDialog;
import com.textdungeon.dialog_control.TraitPickerDialog;
import com.textdungeon.model.Job;
import com.textdungeon.model.Trait;
import com.textdungeon.scenario.ScenarioPack;
import com.textdungeon.scenario.ScenarioRepository;
import com.textdungeon.system.GameSave;
import com.textdungeon.system.UserRecord;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 캐릭터 선택. 고른 직업의 초상화를 크게 보여 주고, 아래 줄에서 직업을 바꾼다.
 * 특성은 직업의 기본 특성으로 시작하고, 카드 옆 바꾸기 버튼의 목록 창에서 다른 특성으로 바꾼다.
 * 잠긴 직업은 이름만 보이고 그림·능력치·설명·특성은 숨긴다. 시작을 누르면 이름을 묻는다.
 */
public class CharacterActivity extends BaseActivity {
    /** 능력치 막대가 가득 차는 값 (가장 높은 직업 능력치 14보다 조금 크게) */
    private static final int STAT_MAX = 15;
    private static final int GOLD = 0xFFE9C176;
    private static final int TEXT = 0xFFE5E2E1;
    private static final int TEXT_LOCKED = 0xFF8D867C;

    private ImageView ivPortrait;
    private View lockedCover, traitSection, btnTraitChange, btnStart;
    private TextView tvJobName, tvTraitBadge, tvDescription, tvTraitName, tvTraitTag, tvTraitDescription;
    private LinearLayout statBars, jobStrip;
    private final Map<Job, FrameLayout> thumbs = new EnumMap<>(Job.class);

    private Job playerJob;
    private Trait selectedTrait;
    private DataControlTower dt;
    private UserRecord record;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_character);

        dt = DataControlTower.getInstance(this);
        record = dt.getUserRecord();

        ivPortrait = findViewById(R.id.iv_job_portrait);
        lockedCover = findViewById(R.id.locked_cover);
        tvJobName = findViewById(R.id.tv_job_name);
        tvTraitBadge = findViewById(R.id.tv_trait_badge);
        tvDescription = findViewById(R.id.character_description);
        statBars = findViewById(R.id.stat_bars);
        jobStrip = findViewById(R.id.job_strip);
        traitSection = findViewById(R.id.trait_section);
        tvTraitName = findViewById(R.id.tv_trait_name);
        tvTraitTag = findViewById(R.id.tv_trait_tag);
        tvTraitDescription = findViewById(R.id.tv_trait_description);
        btnTraitChange = findViewById(R.id.btn_trait_change);
        btnStart = findViewById(R.id.btn_start);

        tvTraitBadge.setBackground(badgeBack(0xBF14100C));
        fitPortrait();
        setSfx(btnStart, btnTraitChange);
        btnTraitChange.setOnClickListener(SingleClickListener.wrap(this::openTraitPicker));
        buildJobStrip();
        // 처음에는 해금된 첫 직업을 골라 둔다
        for (Job job : Job.values()) {
            if (isUnlocked(job)) {
                selectJob(job);
                break;
            }
        }
        btnStart.setOnClickListener(SingleClickListener.wrap(this::startGame)); // 연타 시 새 게임이 두 번 시작되는 것 방지
    }

    private boolean isUnlocked(Job job) {
        return job.defaultUnlocked || record.isUnlockJob(job.name);
    }

    /** 화면이 길어 아래가 비면 그만큼 초상화를 키운다 (정사각형 그림이라 가로 길이까지만) */
    private void fitPortrait() {
        View scroll = findViewById(R.id.character_scroll);
        View content = findViewById(R.id.character_content);
        View hero = findViewById(R.id.hero_frame);
        scroll.post(() -> {
            int extra = scroll.getHeight() - content.getHeight();
            if (extra <= 0) return;
            ViewGroup.LayoutParams lp = hero.getLayoutParams();
            lp.height = Math.min(lp.height + extra, hero.getWidth());
            hero.setLayoutParams(lp);
        });
    }

    // ── 직업 ────────────────────────────────────────────

    /** 아래 줄의 직업 칸. 해금된 직업은 그림과 이름, 잠긴 직업은 자물쇠와 이름만 */
    private void buildJobStrip() {
        int size = dp(72);
        for (Job job : Job.values()) {
            boolean unlocked = isUnlocked(job);
            FrameLayout thumb = new FrameLayout(this);
            GradientDrawable back = new GradientDrawable();
            back.setColor(unlocked ? 0xFF14110E : 0xFF1C1A18);
            back.setCornerRadius(dp(8));
            thumb.setBackground(back);
            thumb.setClipToOutline(true);
            thumb.setContentDescription(job.name);

            if (unlocked) {
                ImageView iv = new ImageView(this);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                GameImages.set(iv, job.img, android.R.drawable.ic_menu_gallery);
                thumb.addView(iv, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            } else {
                ImageView lock = new ImageView(this);
                lock.setImageResource(R.drawable.ic_lock);
                lock.setColorFilter(0xFF5A544C);
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(20), dp(20),
                        Gravity.CENTER_HORIZONTAL | Gravity.TOP);
                lp.topMargin = dp(16);
                thumb.addView(lock, lp);
            }

            TextView name = text(job.name, 10, unlocked ? TEXT : TEXT_LOCKED);
            name.setGravity(Gravity.CENTER);
            name.setSingleLine();
            name.setPadding(0, dp(2), 0, dp(2));
            if (unlocked) name.setBackgroundColor(0x99000000);
            thumb.addView(name, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));

            thumb.setOnClickListener(v -> selectJob(job));
            setSfx(thumb);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMarginEnd(dp(8));
            jobStrip.addView(thumb, lp);
            thumbs.put(job, thumb);
        }
    }

    private void selectJob(Job job) {
        playerJob = job;
        boolean unlocked = isUnlocked(job);
        for (Map.Entry<Job, FrameLayout> e : thumbs.entrySet()) {
            e.getValue().setForeground(frame(e.getKey() == job));
        }
        tvJobName.setText(job.name);
        tvJobName.setTextColor(unlocked ? GOLD : TEXT_LOCKED);
        lockedCover.setVisibility(unlocked ? View.GONE : View.VISIBLE);
        // 잠긴 직업은 이름만. 자리는 남겨 두어 아래 줄이 위아래로 움직이지 않게
        int shown = unlocked ? View.VISIBLE : View.INVISIBLE;
        tvTraitBadge.setVisibility(shown);
        statBars.setVisibility(shown);
        traitSection.setVisibility(shown);
        btnStart.setAlpha(unlocked ? 1f : 0.45f);
        if (!unlocked) {
            ivPortrait.setImageDrawable(null);
            tvDescription.setText("");
            selectedTrait = null;
            return;
        }
        GameImages.set(ivPortrait, job.img, android.R.drawable.ic_menu_gallery);
        tvDescription.setText(job.description);
        showStats(job);
        // 고를 다른 특성이 없으면 바꾸기 버튼을 흐리게 (누르면 늘리는 법을 알려 준다)
        btnTraitChange.setAlpha(availableTraits(job).size() > 1 ? 1f : 0.45f);
        selectTrait(job.trait);
    }

    private void showStats(Job job) {
        statBars.removeAllViews();
        addStatRow("힘", R.drawable.ic_strength, job.strength, 0xFFD9534F);
        addStatRow("민첩", R.drawable.ic_dexterity, job.agility, 0xFF5CB85C);
        addStatRow("체력", R.drawable.ic_health, job.health, 0xFFC9A24A);
        addStatRow("지혜", R.drawable.ic_wisdom, job.wisdom, 0xFF6B8FD6);
    }

    private void addStatRow(String label, int icon, int value, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(3), 0, dp(3));

        ImageView iv = new ImageView(this);
        iv.setImageResource(icon);
        iv.setColorFilter(0xFFB8B0A6);
        row.addView(iv, new LinearLayout.LayoutParams(dp(14), dp(14)));

        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(dp(38), LinearLayout.LayoutParams.WRAP_CONTENT);
        nameLp.setMarginStart(dp(6));
        row.addView(text(label, 12, 0xFFB8B0A6), nameLp);

        // 막대: 전체를 STAT_MAX로 보고 채운 부분만 weight를 준다
        LinearLayout track = new LinearLayout(this);
        track.setBackground(round(0xFF2A2622));
        track.setClipToOutline(true);
        track.setWeightSum(STAT_MAX);
        View fill = new View(this);
        fill.setBackground(round(color));
        track.addView(fill, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT,
                Math.min(value, STAT_MAX)));
        row.addView(track, new LinearLayout.LayoutParams(0, dp(7), 1f));

        TextView num = text(String.valueOf(value), 13, TEXT);
        num.setGravity(Gravity.END);
        row.addView(num, new LinearLayout.LayoutParams(dp(30), LinearLayout.LayoutParams.WRAP_CONTENT));
        statBars.addView(row);
    }

    // ── 특성 ────────────────────────────────────────────

    /** 고른 직업의 기본 특성, 해금된 직업들의 특성, 상점에서 해금한 특성 */
    private List<Trait> availableTraits(Job job) {
        List<Trait> traits = new ArrayList<>();
        traits.add(job.trait);
        for (Job j : Job.values()) {
            if (isUnlocked(j) && !traits.contains(j.trait)) traits.add(j.trait);
        }
        for (Trait t : Trait.values()) {
            if (record.isUnlockTrait(t.name()) && !traits.contains(t)) traits.add(t);
        }
        return traits;
    }

    private void openTraitPicker(View v) {
        if (playerJob == null || !isUnlocked(playerJob)) return;
        List<Trait> traits = availableTraits(playerJob);
        if (traits.size() <= 1) {
            Snackbar.make(v, "직업을 해금하거나 상점에서 특성을 사면 바꿀 수 있습니다", Snackbar.LENGTH_SHORT).show();
            return;
        }
        new TraitPickerDialog(this, playerJob, traits, selectedTrait, this::selectTrait).show();
    }

    private void selectTrait(Trait trait) {
        selectedTrait = trait;
        boolean isDefault = trait == playerJob.trait;
        tvTraitBadge.setText("특성 · " + trait.displayName);
        tvTraitName.setText(trait.displayName);
        tvTraitTag.setText(isDefault ? "기본 특성" : "바꾼 특성");
        tvTraitTag.setTextColor(isDefault ? 0xFFC9A86A : 0xFFD9826A);
        tvTraitDescription.setText(trait.description);
    }

    // ── 시작 ────────────────────────────────────────────

    private void startGame(View v) {
        if (playerJob == null || !isUnlocked(playerJob)) {
            Snackbar.make(v, "해금되지 않은 직업입니다", Snackbar.LENGTH_SHORT).show();
            return;
        }
        new PlayerNameDialog(this, playerJob, selectedTrait, record.getRecentNames(), this::onNameChosen).show();
    }

    private void onNameChosen(String name) {
        record.addRecentName(name);
        GameSave.saveUserRecord(this, record);
        // DLC 시나리오가 있을 때만 고르게 한다. 기본 시나리오뿐이면 바로 시작
        if (dt.getScenarioRepository().hasChoice()) {
            showScenarioPicker(name);
        } else {
            beginRun(name, ScenarioPack.BASE);
        }
    }

    private void beginRun(String playerName, ScenarioPack pack) {
        dt.startNewGame(playerName, playerJob, selectedTrait.name(), pack);

        Intent intent = new Intent(this, DifficultyActivity.class);
        startActivity(intent);
    }

    /** 시나리오 고르기. 잠긴 DLC를 누르면 상점 프리미엄 탭으로 보낸다 */
    private void showScenarioPicker(String playerName) {
        ScenarioRepository repo = dt.getScenarioRepository();
        Entitlements entitlements = dt.getEntitlements();
        List<ScenarioPack> packs = repo.all();
        String[] labels = new String[packs.size()];
        int checked = 0;
        ScenarioPack last = ScenarioPack.fromId(dt.getUserRecord().getLastScenarioId());
        for (int i = 0; i < packs.size(); i++) {
            ScenarioPack p = packs.get(i);
            labels[i] = repo.isUnlocked(p, entitlements) ? p.displayName : "🔒 " + p.displayName;
            if (p == last && repo.isUnlocked(p, entitlements)) checked = i;
        }
        final int[] choice = {checked};
        new android.app.AlertDialog.Builder(this)
                .setTitle("시나리오 선택")
                .setSingleChoiceItems(labels, checked, (d, which) -> choice[0] = which)
                .setPositiveButton("시작", (d, w) -> {
                    ScenarioPack pack = packs.get(choice[0]);
                    if (repo.isUnlocked(pack, entitlements)) {
                        beginRun(playerName, pack);
                    } else {
                        Intent intent = new Intent(this, ShopActivity.class);
                        intent.putExtra(ShopActivity.EXTRA_TAB, ShopActivity.TAB_PREMIUM);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    // ── 그리기 도우미 ────────────────────────────────────

    private TextView text(String s, float sp, int color) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        tv.setTypeface(Typeface.SERIF);
        return tv;
    }

    /** 직업 칸 테두리: 고른 칸은 금색 */
    private GradientDrawable frame(boolean selected) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(8));
        d.setColor(0);
        d.setStroke(dp(selected ? 2 : 1.5f), selected ? GOLD : 0xFF3A342E);
        return d;
    }

    /** 초상화 위 "특성 · X" 배지: 금색 테두리 알약 */
    private GradientDrawable badgeBack(int fill) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(20));
        d.setColor(fill);
        d.setStroke(dp(1), GOLD);
        return d;
    }

    private GradientDrawable round(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(4));
        d.setColor(color);
        return d;
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
