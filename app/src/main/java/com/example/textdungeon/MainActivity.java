package com.example.textdungeon;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.textdungeon.layout_control.BattleLayout;
import com.textdungeon.layout_control.EventLayout;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.start_activity);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    public void Main_moveEvent(View v){
        Intent intent = new Intent(MainActivity.this, EventLayout.class);
        startActivity(intent);
        finish();
    }
    public void Main_moveBattle(View v){
        Intent intent = new Intent(MainActivity.this, BattleLayout.class);
        startActivity(intent);
        finish();
    }
}