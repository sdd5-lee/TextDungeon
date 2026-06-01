package com.textdungeon.dialog_control;

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

import com.example.textdungeon.R;
import com.google.android.material.snackbar.Snackbar;
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

        setContentView(R.layout.dialog_item_shop);

        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvPlayerInfo = findViewById(R.id.tv_player_info);
        tvGold = findViewById(R.id.tv_gold);
        itemContainer = findViewById(R.id.item_container);
        Button btnClose = findViewById(R.id.btn_shop_close);

        tvPlayerInfo.setText(player.getName() + " (Lv." + player.getLevel() + ")");
        updateGoldDisplay();

        renderItems();

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
                View rowView = inflater.inflate(R.layout.row_item_shop, itemContainer, false);

                TextView tvName = rowView.findViewById(R.id.tv_item_name);
                TextView tvPrice = rowView.findViewById(R.id.tv_item_price);
                Button btnBuy = rowView.findViewById(R.id.btn_buy);

                int price = item.getValue();
                tvName.setText(item.getName());
                tvPrice.setText("💰 " + price + " G");

                btnBuy.setOnClickListener(v -> onBuyItem(item, price));

                itemContainer.addView(rowView);
            }
        }
    }

    private void onBuyItem(Item item, int price) {
        if (player.getStat().getGold() < price) {
            showBar("골드가 부족합니다!");
            return;
        }

        if (player.getInventory().isFullItem()) {
            showBar("인벤토리가 가득 찼습니다!");
            return;
        }

        player.getStat().setGold(player.getStat().getGold() - price);
        player.getInventory().addItem(item);

        updateGoldDisplay();

        showBar(item.getName() + "을(를) 구매했습니다!");
    }

    private void updateGoldDisplay() {
        tvGold.setText("💰 보유 골드: " + player.getStat().getGold() + " G");
    }

    private void showBar(String message) {
        View activityRootView = null;
        if (getContext() instanceof android.app.Activity) {
            activityRootView = ((android.app.Activity) getContext()).findViewById(android.R.id.content);
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