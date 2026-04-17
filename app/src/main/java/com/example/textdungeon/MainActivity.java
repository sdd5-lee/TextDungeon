package com.example.textdungeon;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.layout_control.EventLayout;
import com.textdungeon.layout_control.TestLayout;
import com.textdungeon.model.Job;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_activity);

        DataControlTower dt = DataControlTower.getInstance(this);
        dt.initPlayer("player", Job.WARRIOR);
    }
    public void battleMoveMain(View v) {
        Intent intent = new Intent(this, TestLayout.class);
        startActivity(intent);
    }
    public void mainMoveEvent(View v) {
        Intent intent = new Intent(this, EventLayout.class);
        startActivity(intent);
    }
}