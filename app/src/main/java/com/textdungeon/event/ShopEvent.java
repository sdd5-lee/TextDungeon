package com.textdungeon.event;

import android.content.Context;

import com.google.gson.Gson;
import com.textdungeon.data.DataControl;
import com.textdungeon.layout_control.ShopDialog;
import com.textdungeon.model.Item;
import com.textdungeon.player.Player;
import com.textdungeon.system.GameSave;

import java.util.List;
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
    public void openShop(Context context, Player player, DataControl<Item> itemManager) {
        ShopDialog dialog = new ShopDialog(context, player, itemManager, shopItems);
        dialog.show();
    }
}