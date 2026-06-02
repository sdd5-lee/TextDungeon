package com.textdungeon.activity_control;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.textdungeon.R;
import com.google.android.material.snackbar.Snackbar;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.model.Job;
import com.textdungeon.model.Trait;
import com.textdungeon.system.UserRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CharacterActivity extends BaseActivity {
    FrameLayout btnEmbark;
    TextView characterDesc, statHp, statStr, statAgi, statWis;
    EditText editPlayerName;

    // 특성 아코디언 관련 뷰
    LinearLayout btnTraitToggle;
    GridLayout traitGridContainer;
    TextView tvSelectedTrait;
    ImageView ivTraitArrow;
    View traitScrollArea;
    boolean isTraitExpanded = false;
    Map<Job, LinearLayout> jobViews = new HashMap<>();

    Job playerJob;
    Trait selectedTrait;
    DataControlTower dt;
    UserRecord record;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_character);

        dt = DataControlTower.getInstance(this);
        record = dt.getUserRecord();

        LinearLayout jobKnight = findViewById(R.id.job_knight);
        LinearLayout jobRogue = findViewById(R.id.job_rogue);
        LinearLayout jobMage = findViewById(R.id.job_mage);
        LinearLayout jobWarrior = findViewById(R.id.job_warrior);
        LinearLayout jobPaladin = findViewById(R.id.job_paladin);
        LinearLayout jobRanger = findViewById(R.id.job_ranger);
        LinearLayout jobHero = findViewById(R.id.job_hero);
        LinearLayout jobWarlock = findViewById(R.id.job_warlock);
        LinearLayout jobMonk = findViewById(R.id.job_monk);

        // Map에 직업 뷰 등록 (성기사 매핑 유지)
        jobViews.put(Job.KNIGHT, jobKnight);
        jobViews.put(Job.ROGUE, jobRogue);
        jobViews.put(Job.MAGE, jobMage);
        jobViews.put(Job.WARRIOR, jobWarrior);
        jobViews.put(Job.PALADIN, jobPaladin);
        jobViews.put(Job.ARCHER, jobRanger);
        jobViews.put(Job.HERO, jobHero);
        jobViews.put(Job.WARLOCK, jobWarlock);
        jobViews.put(Job.MONK, jobMonk);

        statHp = findViewById(R.id.stat_hp);
        statStr = findViewById(R.id.stat_str);
        statAgi = findViewById(R.id.stat_agi);
        statWis = findViewById(R.id.stat_wis);
        editPlayerName = findViewById(R.id.edit_player_name);
        characterDesc = findViewById(R.id.character_description);
        btnEmbark = findViewById(R.id.btn_embark);

        // 아코디언 뷰 초기화
        btnTraitToggle = findViewById(R.id.btn_trait_toggle);
        tvSelectedTrait = findViewById(R.id.tv_selected_trait);
        ivTraitArrow = findViewById(R.id.iv_trait_arrow);
        traitScrollArea = findViewById(R.id.trait_scroll_area);
        traitGridContainer = findViewById(R.id.trait_grid_container);

        setSfx(jobKnight, jobRogue, jobMage, jobWarrior, jobPaladin, jobRanger, jobHero, jobWarlock, jobMonk, btnEmbark, btnTraitToggle);

        initJobCards();

        jobRogue.setOnClickListener(v -> handleJobClick(v, Job.ROGUE));
        jobMage.setOnClickListener(v -> handleJobClick(v, Job.MAGE));
        jobWarrior.setOnClickListener(v -> handleJobClick(v, Job.WARRIOR));
        jobRanger.setOnClickListener(v -> handleJobClick(v, Job.ARCHER));

        jobKnight.setOnClickListener(v -> handleJobClick(v, Job.KNIGHT));
        jobPaladin.setOnClickListener(v -> handleJobClick(v, Job.PALADIN));
        jobHero.setOnClickListener(v -> handleJobClick(v, Job.HERO));
        jobWarlock.setOnClickListener(v -> handleJobClick(v, Job.WARLOCK));
        jobMonk.setOnClickListener(v -> handleJobClick(v, Job.MONK));

        // 아코디언 토글 이벤트
        btnTraitToggle.setOnClickListener(v -> toggleTraitMenu());

        statHp.setText("");
        statStr.setText("");
        statAgi.setText("");
        statWis.setText("");
        characterDesc.setText("직업을 선택하세요");
        playerJob = null;

        btnEmbark.setOnClickListener(this::startGame);
    }

    private void initJobCards() {
        for (Map.Entry<Job, LinearLayout> entry : jobViews.entrySet()) {
            Job job = entry.getKey();
            LinearLayout layout = entry.getValue();
            ImageView iv = (ImageView) layout.getChildAt(0);
            TextView tv = (TextView) layout.getChildAt(1);

            if (!job.defaultUnlocked && !record.isUnlockJob(job.name)) {
                layout.setAlpha(0.3f);
                iv.setImageResource(android.R.drawable.ic_secure);
                iv.setColorFilter(Color.parseColor("#888888"));
                tv.setText("???");
                tv.setTextColor(Color.parseColor("#888888"));
            } else {
                layout.setAlpha(1.0f);
                int imgResId = getResources().getIdentifier(job.img, "drawable", getPackageName());
                if (imgResId != 0) {
                    iv.setImageResource(imgResId);
                } else {
                    iv.setImageResource(android.R.drawable.ic_menu_gallery);
                }
                iv.clearColorFilter();
                tv.setText(job.name);
            }
        }
    }

    private void handleJobClick(View v, Job job) {
        if (record.isUnlockJob(job.name)) updateUI(job);
        else Snackbar.make(v, "해금되지 않은 직업입니다", Snackbar.LENGTH_SHORT).show();
    }

    private void updateUI(Job job) {
        playerJob = job;
        selectedTrait = job.trait;

        highlightSelectedJob();

        statHp.setText(String.valueOf(job.health));
        statStr.setText(String.valueOf(job.strength));
        statAgi.setText(String.valueOf(job.agility));
        statWis.setText(String.valueOf(job.wisdom));

        btnTraitToggle.setVisibility(View.VISIBLE);
        tvSelectedTrait.setText(selectedTrait.displayName);
        updateDescription();

        if (isTraitExpanded) toggleTraitMenu();
        populateTraitList();
    }

    private void highlightSelectedJob() {
        for (Map.Entry<Job, LinearLayout> entry : jobViews.entrySet()) {
            Job job = entry.getKey();
            LinearLayout layout = entry.getValue();

            if (!job.defaultUnlocked && !record.isUnlockJob(job.name)) {
                continue;
            }

            TextView tv = (TextView) layout.getChildAt(1);

            if (job == playerJob) {
                tv.setTextColor(Color.parseColor("#E9C176"));
                layout.setBackgroundColor(Color.parseColor("#3A3A3A"));
            } else {
                tv.setTextColor(Color.parseColor("#E5E2E1"));
                layout.setBackgroundColor(Color.parseColor("#2A2A2A"));
            }
        }
    }

    private void updateDescription() {
        if (playerJob != null && selectedTrait != null) {
            characterDesc.setText(playerJob.description + "\n\n[장착 중인 특성: " + selectedTrait.displayName + "]\n" + selectedTrait.description);
        }
    }

    private void toggleTraitMenu() {
        isTraitExpanded = !isTraitExpanded;
        if (isTraitExpanded) {
            traitScrollArea.setVisibility(View.VISIBLE);
            ivTraitArrow.animate().rotation(180).setDuration(200).start();
        } else {
            traitScrollArea.setVisibility(View.GONE);
            ivTraitArrow.animate().rotation(0).setDuration(200).start();
        }
    }

    private void populateTraitList() {
        traitGridContainer.removeAllViews();
        List<Trait> availableTraits = new ArrayList<>();

        // 직업의 기본 특성
        availableTraits.add(playerJob.trait);
        for (Job j : Job.values()) {
            if (j.defaultUnlocked || record.isUnlockJob(j.name)) {
                if (!availableTraits.contains(j.trait)) {
                    availableTraits.add(j.trait);
                }
            }
        }
        // 해금된 상점 특성
        for (Trait t : Trait.values()) {
            if (record.isUnlockTrait(t.name()) && !availableTraits.contains(t)) {
                availableTraits.add(t);
            }
        }

        for (Trait t : availableTraits) {
            LinearLayout block = new LinearLayout(this);
            android.widget.GridLayout.LayoutParams params = new android.widget.GridLayout.LayoutParams();
            params.width = 0;
            params.height = android.widget.GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f);
            params.setMargins(8, 8, 8, 8);
            block.setLayoutParams(params);

            block.setOrientation(LinearLayout.VERTICAL);
            block.setGravity(android.view.Gravity.CENTER);
            block.setPadding(8, 32, 8, 32);

            if (t == selectedTrait) {
                block.setBackgroundColor(Color.parseColor("#3A3A3A"));
            } else {
                block.setBackgroundColor(Color.parseColor("#2A2A2A"));
            }

            TextView tv = new TextView(this);
            tv.setText(t.displayName + "");
            tv.setTextColor(Color.parseColor(t == selectedTrait ? "#E9C176" : "#E5E2E1"));
            tv.setTextSize(11f);
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setTypeface(android.graphics.Typeface.SERIF, t == selectedTrait ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

            block.addView(tv);

            block.setClickable(true);
            block.setOnClickListener(v -> {
                selectedTrait = t;
                tvSelectedTrait.setText(t.displayName);
                updateDescription();
                populateTraitList();
            });

            traitGridContainer.addView(block);
        }
    }

    private void startGame(View v) {
        if (playerJob != null) {
            String inputName = editPlayerName.getText().toString().trim();
            String playerName = inputName.isEmpty() ? "모험가 A" : inputName;

            dt.startNewGame(playerName, playerJob, selectedTrait.name());

            Intent intent = new Intent(this, DifficultyActivity.class);
            startActivity(intent);
        } else {
            Snackbar.make(v, "직업을 선택하지 않았습니다", Snackbar.LENGTH_SHORT).show();
        }
    }
}