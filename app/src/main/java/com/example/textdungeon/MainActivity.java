package com.example.textdungeon;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.textdungeon.data.DataControlTower;
import com.textdungeon.layout_control.CharacterLayout;
import com.textdungeon.layout_control.EventLayout;
import com.textdungeon.layout_control.TestLayout;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.title_layout);

        DataControlTower dt = DataControlTower.getInstance(this);
        if(dt.getPlayer() == null){
            findViewById(R.id.btn_start).setOnClickListener(this::MoveCharacter);
        }
        else{
            findViewById(R.id.btn_start).setOnClickListener(this::MoveEvent);
        }
        findViewById(R.id.btn_shop).setOnClickListener(this::MoveShop);
        findViewById(R.id.btn_option).setOnClickListener(this::MoveOption);
    }
    public void MoveCharacter(View v) {
        Intent intent = new Intent(this, CharacterLayout.class);
        startActivity(intent);
    }
    public void MoveEvent(View v) {
        Intent intent = new Intent(this, EventLayout.class);
        intent.putExtra("IS_NEW_GAME", false);
        startActivity(intent);
    }
    public void MoveShop(View v) {
        Intent intent = new Intent(this, TestLayout.class);//임시로 테스트 레이아웃 창 이동
        startActivity(intent);
    }
    public void MoveOption(View v) {
        Intent intent = new Intent(this, CharacterLayout.class);
        startActivity(intent);
    }
}