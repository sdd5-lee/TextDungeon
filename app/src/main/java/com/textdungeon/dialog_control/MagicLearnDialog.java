package com.textdungeon.dialog_control;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.textdungeon.R;
import com.google.android.material.snackbar.Snackbar;
import com.textdungeon.activity_control.BaseActivity;
import com.textdungeon.data.DataControl;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.model.Achievement;
import com.textdungeon.model.Magic;
import com.textdungeon.player.Player;

import java.util.List;

public class MagicLearnDialog extends Dialog {

    private final Player player;
    private final DataControl<Magic> magicManager;

    private TextView tvCurrentWisdom;
    private LinearLayout magicContainer;

    public MagicLearnDialog(Context context, Player player, DataControl<Magic> magicManager) {
        super(context);
        this.player = player;
        this.magicManager = magicManager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_magic_scroll);

        // 배경 투명화 및 꽉 차게 설정
        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        tvCurrentWisdom = findViewById(R.id.tv_current_wisdom);
        magicContainer = findViewById(R.id.magic_container);
        Button btnClose = findViewById(R.id.btn_shop_close);

        tvCurrentWisdom.setText("나의 현재 지혜: " + player.getStat().getWisdom());
        // 마법 목록 렌더링
        renderMagics();

        // 닫기 버튼
        btnClose.setOnClickListener(v -> dismiss());
    }

    private void renderMagics() {
        magicContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        List<Magic> allMagics = magicManager.getAll();

        if (allMagics.isEmpty()) {
            TextView emptyText = new TextView(getContext());
            emptyText.setText("현재 배울 수 있는 마법이 없습니다.");
            emptyText.setTextColor(0xFF888888);
            emptyText.setPadding(0, 40, 0, 40);
            magicContainer.addView(emptyText);
            return;
        }

        for (Magic magic : allMagics) {
            View rowView = inflater.inflate(R.layout.row_magic_scroll, magicContainer, false);

            ImageView ivMagicIcon = rowView.findViewById(R.id.iv_magic_icon);
            TextView tvName = rowView.findViewById(R.id.tv_magic_name);
            TextView tvReqWisdom = rowView.findViewById(R.id.tv_req_wisdom);
            Button btnLearn = rowView.findViewById(R.id.btn_learn);

            String imgName = magic.getImg();
            if (imgName != null && !imgName.isEmpty()) {
                int resId = getContext().getResources().getIdentifier(
                        imgName, "drawable", getContext().getPackageName());

                if (resId != 0) {
                    ivMagicIcon.setImageResource(resId);
                } else {
                    ivMagicIcon.setImageResource(R.drawable.magic_default_icon);
                }
            }

            int reqWisdom = magic.getValue();
            int currentWisdom = player.getStat().getWisdom();

            tvName.setText(magic.getName());
            tvReqWisdom.setText("요구 지혜: " + reqWisdom);

            if(player.getMagicScroll().hasMagic(magic.getId())){
                tvReqWisdom.setTextColor(0xFFE57373);
                tvName.setTextColor(0xFFE57373);
                tvReqWisdom.setText("이미 배운 마법입니다.");
                btnLearn.setVisibility(View.INVISIBLE);
            }
            else if (currentWisdom >= reqWisdom) {
                tvReqWisdom.setTextColor(0xFF81C784);
                btnLearn.setBackgroundColor(0xFF9C27B0);
                btnLearn.setOnClickListener(v -> onLearnMagic(magic));
            } else {
                tvReqWisdom.setTextColor(0xFFE57373);
                btnLearn.setBackgroundColor(0xFF666666);

                btnLearn.setOnClickListener(v ->
                        showBar("지혜가 부족하여 해독할 수 없습니다.")
                );
            }

            magicContainer.addView(rowView);
        }
    }

    private void onLearnMagic(Magic magic) {
        player.getMagicScroll().addMagic(magic.getId(), magic.getMaxCount());
        showBar(magic.getName() + " 마법의 이치를 깨달았습니다!");
        List<Achievement> unlocked = DataControlTower.getInstance(getContext()).getAchievementManager().updateProgress("magic_learn", 1, true);

        if (getContext() instanceof BaseActivity) {
            ((BaseActivity) getContext()).showAchievementNotification(unlocked);
        }
        dismiss();
    }
    private void showBar(String message) {
        View activityRootView = null;
        if (getContext() instanceof Activity) {
            activityRootView = ((Activity) getContext()).findViewById(android.R.id.content);
        } else if (getWindow() != null) {
            activityRootView = getWindow().getDecorView();
        }

        if (activityRootView != null) {
            Snackbar snackbar = Snackbar.make(activityRootView, message, Snackbar.LENGTH_SHORT);
            snackbar.setBackgroundTint(Color.parseColor("#901418"));
            snackbar.setTextColor(Color.parseColor("#E5E2E1"));

            snackbar.show();
        }
    }
}