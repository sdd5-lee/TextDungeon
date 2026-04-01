package com.textdungeon.layout_control;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.textdungeon.MainActivity;
import com.example.textdungeon.R;
import com.textdungeon.player.Player;
import com.textdungeon.player.Job;
import com.textdungeon.system.GameSave;

public class BattleLayout extends AppCompatActivity {
    GameSave gameSave = new GameSave(1,new Player("테스트용", Job.WARRIOR));
    Player player = gameSave.getPlayer();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.battle_activity);

        TextView textView = findViewById(R.id.inv);
        Button testbutton = findViewById(R.id.test);
        testbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.pickUpItem("임시아이템");
                for (String s : player.getInventory()) {
                    if (player.getInventory().isEmpty()) {
                        textView.setText("인벤토리 : "+s);
                        continue;
                    }
                    textView.setText(textView.getText()+","+s);
                }
            }
        });


        Button save = findViewById(R.id.savebutton);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(BattleLayout.this, "저장되었습니다 ", Toast.LENGTH_SHORT).show();
                gameSave.save(BattleLayout.this);
            }
        });
        Button load = findViewById(R.id.loadbutton);
        load.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(BattleLayout.this, "게임을 불러왔습니다", Toast.LENGTH_SHORT).show();
                gameSave = GameSave.load(BattleLayout.this, 1);
                if (gameSave != null) {
                    player = gameSave.getPlayer(); // 플레이어 객체도 동기화
                }
                for (String s : player.getInventory()) {
                    if (player.getInventory().isEmpty()) {
                        textView.setText("인벤토리 : "+s);
                        continue;
                    }
                    textView.setText(textView.getText()+","+s);
                }
            }
        });
    }
    public void battleMoveMain(View v){
        Intent intent = new Intent(BattleLayout.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    public void battleMoveEvent(View v){
        Intent intent = new Intent(BattleLayout.this, EventLayout.class);
        startActivity(intent);
        finish();
    }
}
