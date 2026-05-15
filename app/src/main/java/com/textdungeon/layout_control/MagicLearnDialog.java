package com.textdungeon.layout_control;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.textdungeon.R; // 실제 패키지명에 맞게 확인해주세요!
import com.textdungeon.data.DataControl;
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
        setContentView(R.layout.magic_scroll_dialog);

        // 배경 투명화 및 꽉 차게 설정
        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        tvCurrentWisdom = findViewById(R.id.tv_current_wisdom);
        magicContainer = findViewById(R.id.magic_container);
        Button btnClose = findViewById(R.id.btn_close);

        // 현재 지혜 표시 (가져오기)
        tvCurrentWisdom.setText("💡 나의 현재 지혜: " + player.getStat().getWisdom());
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
            View rowView = inflater.inflate(R.layout.magic_scroll_row, magicContainer, false);

            TextView tvName = rowView.findViewById(R.id.tv_magic_name);
            TextView tvReqWisdom = rowView.findViewById(R.id.tv_req_wisdom);
            Button btnLearn = rowView.findViewById(R.id.btn_learn);

            int reqWisdom = magic.getValue();
            int currentWisdom = player.getStat().getWisdom();

            tvName.setText(magic.getName());
            tvReqWisdom.setText("요구 지혜: " + reqWisdom);

            if (currentWisdom >= reqWisdom) {
                tvReqWisdom.setTextColor(0xFF81C784); // 초록색 (조건 충족)
                btnLearn.setBackgroundColor(0xFF9C27B0); // 보라색 (배우기 가능)
                btnLearn.setOnClickListener(v -> onLearnMagic(magic));
            } else {
                tvReqWisdom.setTextColor(0xFFE57373); // 빨간색 (조건 미달)
                btnLearn.setBackgroundColor(0xFF666666); // 회색 (비활성화)
                btnLearn.setOnClickListener(v ->
                        Toast.makeText(getContext(), "지혜가 부족하여 해독할 수 없습니다.", Toast.LENGTH_SHORT).show()
                );
            }

            magicContainer.addView(rowView);
        }
    }

    private void onLearnMagic(Magic magic) {
        player.getMagicScroll().addMagic(magic.getId(), magic.getMaxCount());

        Toast.makeText(getContext(), magic.getName() + " 마법의 이치를 깨달았습니다!", Toast.LENGTH_SHORT).show();
        dismiss(); // 다이얼로그 닫기
    }
}