package com.example.textdungeon;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.textdungeon.data.DataControlTower;
import com.textdungeon.layout_control.CharacterLayout;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.title_layout);

        DataControlTower dt = DataControlTower.getInstance(this);
        findViewById(R.id.btn_start).setOnClickListener(this::MoveCharacter);
    }
    public void MoveCharacter(View v) {
        Intent intent = new Intent(this, CharacterLayout.class);
        startActivity(intent);
    }
    public void MoveShop(View v) {
        Intent intent = new Intent(this, CharacterLayout.class);
        startActivity(intent);
    }
    public void MoveOption(View v) {
        Intent intent = new Intent(this, CharacterLayout.class);
        startActivity(intent);
    }
}