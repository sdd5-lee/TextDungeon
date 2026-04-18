package com.textdungeon.event;

import com.google.gson.Gson;
import com.textdungeon.model.Item;
import com.textdungeon.player.Player;

public class ShopEvent extends GameEvent{
    private String itemId;

    public ShopEvent() {
        super();
    }
    public static ShopEvent createFromJson(String json) {
        return new Gson().fromJson(json, ShopEvent.class);
    }
    public Item buyItem(Player player){

        return null;
    }

}
