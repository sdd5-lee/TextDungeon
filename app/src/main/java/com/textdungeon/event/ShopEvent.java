package com.textdungeon.event;

import android.content.Context;

import com.google.gson.Gson;
import com.textdungeon.data.DataControl;
import com.textdungeon.layout_control.ShopDialog;
import com.textdungeon.model.Item;
import com.textdungeon.player.Player;

import java.util.List;

/**
 * 상점 이벤트 — 플레이어가 아이템을 구매할 수 있는 이벤트
 */
public class ShopEvent extends GameEvent {
    private List<String> shopItems;

    public ShopEvent() {
        super();
    }

    public static ShopEvent createFromJson(String json) {
        return new Gson().fromJson(json, ShopEvent.class);
    }

    public List<String> getShopItems() {
        return shopItems;
    }

    /**
     * 상점 다이얼로그를 연다.
     * EventLayout에서 선택지 클릭 시 호출된다.
     */
    public void openShop(Context context, Player player, DataControl<Item> itemManager) {
        ShopDialog dialog = new ShopDialog(context, player, itemManager, shopItems);
        dialog.show();
    }
}