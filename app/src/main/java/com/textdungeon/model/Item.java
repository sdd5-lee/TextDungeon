package com.textdungeon.model;

import com.google.gson.Gson;
import com.textdungeon.player.Player;

public class Item {
    private String name;
    private String id;
    private String type;
    private int hp;
    private int atk;
    private boolean isConsumable;

    private Item() {}
    public static Item createFromJson(String json) {
        return new Gson().fromJson(json, Item.class);
    }

    public String getName() { return name; }
    public String getId() { return id; }

    public int getAtk() {
        return atk;
    }

    public int getHp() {
        return hp;
    }

    public String getType() {
        return type;
    }


    public Boolean itemUse(Player player) {
        if (!"consumables".equals(this.type)){
            return false;
        }
        if (this.hp > 0) {
            player.heal(this.hp);
        }
        return true;
    }
}
