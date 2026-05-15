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

import com.example.textdungeon.R; // R 임포트 주의! (본인 프로젝트 패키지명에 맞게 수정하세요)
import com.textdungeon.data.DataControl;
import com.textdungeon.model.Item;
import com.textdungeon.player.Player;

import java.util.List;

public class ShopDialog extends Dialog {

    private final Player player;
    private final DataControl<Item> itemManager;
    private final List<String> shopItemIds;

    private TextView tvGold;
    private LinearLayout itemContainer;

    public ShopDialog(Context context, Player player, DataControl<Item> itemManager, List<String> shopItemIds) {
        super(context);
        this.player = player;
        this.itemManager = itemManager;
        this.shopItemIds = shopItemIds;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. 우리가 만든 XML 레이아웃을 연결!
        setContentView(R.layout.inshop_dialog);

        // 다이얼로그 크기 및 배경 설정
        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // 2. XML에 있는 View들 가져오기
        TextView tvPlayerInfo = findViewById(R.id.tv_player_info);
        tvGold = findViewById(R.id.tv_gold);
        itemContainer = findViewById(R.id.item_container);
        Button btnClose = findViewById(R.id.btn_close);

        // 3. 데이터 셋팅
        tvPlayerInfo.setText(player.getName() + " (Lv." + player.getLevel() + ")");
        updateGoldDisplay();

        // 4. 아이템 목록 렌더링
        renderItems();

        // 5. 뒤로가기 버튼 이벤트
        btnClose.setOnClickListener(v -> dismiss());
    }

    private void renderItems() {
        itemContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        if (shopItemIds == null || shopItemIds.isEmpty()) {
            TextView emptyText = new TextView(getContext());
            emptyText.setText("판매할 상품이 없습니다.");
            emptyText.setTextColor(0xFF888888);
            emptyText.setPadding(0, 40, 0, 40);
            itemContainer.addView(emptyText);
            return;
        }

        for (String itemId : shopItemIds) {
            Item item = itemManager.spawn(itemId);
            if (item != null) {
                // 개별 아이템 XML(붕어빵 틀)을 메모리로 불러와서 찍어냄 (붕어빵 생성)
                View rowView = inflater.inflate(R.layout.inshop_item_row, itemContainer, false);

                // 찍어낸 붕어빵 안에서 텍스트뷰와 버튼을 찾음
                TextView tvName = rowView.findViewById(R.id.tv_item_name);
                TextView tvPrice = rowView.findViewById(R.id.tv_item_price);
                Button btnBuy = rowView.findViewById(R.id.btn_buy);

                // 데이터 넣기
                int price = item.getValue(); // 임시로 value를 가격으로 사용
                tvName.setText(item.getName());
                tvPrice.setText("💰 " + price + " G");

                // 구매 버튼 이벤트
                btnBuy.setOnClickListener(v -> onBuyItem(item, price));

                // 완성된 한 줄을 컨테이너에 추가
                itemContainer.addView(rowView);
            }
        }
    }

    private void onBuyItem(Item item, int price) {
        if (player.getStat().getGold() < price) {
            Toast.makeText(getContext(), "골드가 부족합니다!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (player.getInventory().isFullItem()) {
            Toast.makeText(getContext(), "인벤토리가 가득 찼습니다!", Toast.LENGTH_SHORT).show();
            return;
        }

        player.getStat().setGold(player.getStat().getGold() - price);
        player.getInventory().addItem(item);

        updateGoldDisplay();
        Toast.makeText(getContext(), item.getName() + "을(를) 구매했습니다!", Toast.LENGTH_SHORT).show();
    }

    private void updateGoldDisplay() {
        tvGold.setText("💰 보유 골드: " + player.getStat().getGold() + " G");
    }
}