package com.textdungeon.model;

import com.google.gson.Gson;
import com.textdungeon.player.Player;

public class Item {
    private String name;
    private String id;
    private String type;
    private int hp;
    private int atk;

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
        if ("consumables".equals(this.type)) {

            if (this.id.equals("item_3")) {
                player.heal(this.hp);
            }
            //else if (this.id.equals("item_02")) {
            //}
            return true;
        }
        return false;
    }
}
