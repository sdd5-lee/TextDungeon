package com.textdungeon.event;

import com.google.gson.Gson;

public class ShopEvent extends GameEvent{
    private String enemyId;
    public ShopEvent() {
        super();
    }
    public static ShopEvent createFromJson(String json) {
        return new Gson().fromJson(json, ShopEvent.class);
    }
}
