package com.textdungeon.layout_control;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.textdungeon.MainActivity;
import com.example.textdungeon.R;

public class BattleLayout extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.battle_activity);
    }
    public void Battle_moveMain(View v){
        Intent intent = new Intent(BattleLayout.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    public void Battle_moveEvent(View v){
        Intent intent = new Intent(BattleLayout.this, EventLayout.class);
        startActivity(intent);
        finish();
    }
}
