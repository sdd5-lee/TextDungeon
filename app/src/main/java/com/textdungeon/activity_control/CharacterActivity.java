package com.textdungeon.activity_control;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.textdungeon.R;
import com.google.android.material.snackbar.Snackbar;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.model.Job;
import com.textdungeon.system.UserRecord;

public class CharacterActivity extends BaseActivity {
    FrameLayout btnEmbark;
    TextView characterDesc;
    TextView statHp, statStr, statAgi, statWis;
    EditText editPlayerName;

    Job playerJob;
    DataControlTower dt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_character);

        LinearLayout jobKnight = findViewById(R.id.job_knight);
        LinearLayout jobRogue = findViewById(R.id.job_rogue);
        LinearLayout jobMage = findViewById(R.id.job_mage);
        LinearLayout jobWarrior = findViewById(R.id.job_warrior);
        LinearLayout jobCleric = findViewById(R.id.job_cleric);
        LinearLayout jobRanger = findViewById(R.id.job_ranger);
        LinearLayout jobHero = findViewById(R.id.job_paladin);
        LinearLayout jobWarlock = findViewById(R.id.job_warlock);
        LinearLayout jobMonk = findViewById(R.id.job_monk);

        statHp = findViewById(R.id.stat_hp);
        statStr = findViewById(R.id.stat_str);
        statAgi = findViewById(R.id.stat_agi);
        statWis = findViewById(R.id.stat_wis);

        editPlayerName = findViewById(R.id.edit_player_name);
        characterDesc = findViewById(R.id.character_description);
        btnEmbark = findViewById(R.id.btn_embark);

        setSfx(jobKnight, jobRogue, jobMage, jobWarrior, jobCleric, jobRanger, jobHero, jobWarlock, jobMonk, btnEmbark);

        dt = DataControlTower.getInstance(this);
        UserRecord record = dt.getUserRecord();

        jobRogue.setOnClickListener(v -> updateUI(Job.ROGUE));
        jobMage.setOnClickListener(v -> updateUI(Job.MAGE));
        jobWarrior.setOnClickListener(v -> updateUI(Job.WARRIOR));
        jobRanger.setOnClickListener(v -> updateUI(Job.ARCHER));

        jobKnight.setOnClickListener(v -> {
            if (record.isUnlockJob(Job.KNIGHT.name)) updateUI(Job.KNIGHT);
            else Snackbar.make(v, "해금되지 않은 직업입니다", Snackbar.LENGTH_SHORT).show();
        });
        jobCleric.setOnClickListener(v -> {
            if (record.isUnlockJob(Job.CLERIC.name)) updateUI(Job.CLERIC);
            else Snackbar.make(v, "해금되지 않은 직업입니다", Snackbar.LENGTH_SHORT).show();
        });
        jobHero.setOnClickListener(v -> {
            if (record.isUnlockJob(Job.HERO.name)) updateUI(Job.HERO);
            else Snackbar.make(v, "해금되지 않은 직업입니다", Snackbar.LENGTH_SHORT).show();
        });
        jobWarlock.setOnClickListener(v -> {
            if (record.isUnlockJob(Job.WARLOCK.name)) updateUI(Job.WARLOCK);
            else Snackbar.make(v, "해금되지 않은 직업입니다", Snackbar.LENGTH_SHORT).show();
        });
        jobMonk.setOnClickListener(v -> {
            if (record.isUnlockJob(Job.MONK.name)) updateUI(Job.MONK);
            else Snackbar.make(v, "해금되지 않은 직업입니다", Snackbar.LENGTH_SHORT).show();
        });

        statHp.setText("");
        statStr.setText("");
        statAgi.setText("");
        statWis.setText("");
        characterDesc.setText("직업을 선택하세요");
        playerJob = null;

        btnEmbark.setOnClickListener(this::startGame);
    }

    private void startGame(View v) {
        if (playerJob != null) {
            String inputName = editPlayerName.getText().toString().trim();
            String playerName = inputName.isEmpty() ? "모험가 A" : inputName;
            dt.startNewGame(playerName, playerJob);
            Intent intent = new Intent(this, DifficultyActivity.class);
            startActivity(intent);
        } else {
            Snackbar.make(v, "직업을 선택하지 않았습니다", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void updateUI(Job job) {
        statHp.setText(String.valueOf(job.health));
        statStr.setText(String.valueOf(job.strength));
        statAgi.setText(String.valueOf(job.agility));
        statWis.setText(String.valueOf(job.wisdom));
        characterDesc.setText(job.description);
        playerJob = job;
    }
}