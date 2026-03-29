package com.textdungeon.layout_control;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.textdungeon.MainActivity;
import com.example.textdungeon.R;

public class EventLayout extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_layout);
    }
    public void Event_moveMain(View v){
        Intent intent = new Intent(EventLayout.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    public void Event_moveBattle(View v){
        android.widget.Toast.makeText(this, "전투 화면으로 이동 시도!", android.widget.Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(EventLayout.this, BattleLayout.class);
        startActivity(intent);
        finish();
    }
}
