package com.textdungeon.layout_control;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;


import com.example.textdungeon.R;
import com.google.android.material.snackbar.Snackbar;
import com.textdungeon.model.Stat;
import com.textdungeon.player.Player;

public class StatDialog extends Dialog {
    private final Player player;
    private TextView statPoint;
    private Stat dialogStat;
    private int str,agi,vit,wis,count;
    public StatDialog(Context context, Player player) {
        super(context);
        this.player = player;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.stats_dialog);
        setCancelable(false);
        setCanceledOnTouchOutside(false);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        dialogStat = player.getStat();
        statPoint = findViewById(R.id.tv_remain_points);
        // 힘 민첩 체력 지능 업 버튼
        ImageButton buttonStr = findViewById(R.id.btn_str_up);
        buttonStr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (count > 0){
                    str++;
                    count--;
                    updateUI();
                }else {
                    Snackbar.make(v, "남은 스텟 포인트가 부족합니다!", Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        ImageButton buttonAgi = findViewById(R.id.btn_agi_up);
        buttonAgi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (count > 0){
                    agi++;
                    count--;
                    updateUI();
                }else {
                    Snackbar.make(v, "남은 스텟 포인트가 부족합니다!", Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        ImageButton buttonVit = findViewById(R.id.btn_vit_up);
        buttonVit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (count > 0){
                    vit++;
                    count--;
                    updateUI();
                }else {
                    Snackbar.make(v, "남은 스텟 포인트가 부족합니다!", Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        ImageButton buttonWis = findViewById(R.id.btn_wis_up);
        buttonWis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (count > 0){
                    wis++;
                    count--;
                    updateUI();
                }else {
                    Snackbar.make(v, "남은 스텟 포인트가 부족합니다!", Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        // 초기화 ,완료 버튼
        Button resetButton = findViewById(R.id.btn_reset_stat);
        resetButton.setOnClickListener(v -> reset());

        Button checkButton = findViewById(R.id.btn_confirm_stat);
        checkButton.setOnClickListener(v -> confirm());

        reset();
    }
    private void reset(){
        str = dialogStat.getStrength();
        agi = dialogStat.getAgility();
        vit = dialogStat.getHealth();
        wis = dialogStat.getWisdom();
        count = dialogStat.getStatPoint();
        updateUI();

    }
    private void updateUI(){
        statPoint.setText("남은 스텟 포인트 : " + count);
        ((TextView) findViewById(R.id.tv_str_val)).setText(String.valueOf(str));
        ((TextView) findViewById(R.id.tv_agi_val)).setText(String.valueOf(agi));
        ((TextView) findViewById(R.id.tv_vit_val)).setText(String.valueOf(vit));
        ((TextView) findViewById(R.id.tv_wis_val)).setText(String.valueOf(wis));
    }
    // 완료 버튼 클릭 시 실행되는 함수
    private void confirm() {
        if (count > 0) {
            showRemindDialog();
        } else {
            saveAndDismiss();
        }
    }
    private void showRemindDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());

        builder.setTitle("포인트 배분 확인");
        builder.setMessage("남은 스텟 포인트가 있습니다.\n배분하지 않고 완료하시겠습니까?\n(다음 레벨업 전까지 배분할 수 없습니다.)");

        builder.setPositiveButton("예", (dialog, which) -> {
            saveAndDismiss();
        });

        builder.setNegativeButton("아니오", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.show();
    }
    private void saveAndDismiss() {
        dialogStat.setStrength(str);
        dialogStat.setAgility(agi);
        dialogStat.setHealth(vit);
        dialogStat.setWisdom(wis);
        dialogStat.setStatPoint(count);

        dismiss();
    }
    @Override
    protected void onStart() {
        super.onStart();
        Window window = getWindow();
        if (window != null) {
            int width = (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.95);
            int height = (int) (getContext().getResources().getDisplayMetrics().heightPixels * 0.90);
            window.setLayout(width, height);
        }
    }
}
