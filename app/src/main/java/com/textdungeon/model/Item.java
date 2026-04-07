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

    //타입은 소모품 consumables, 특별한 아이템 artifact, 무기 weapon,갑옷 armor
    public void itemUse(Player player) {
        switch (type) {
            case "consumables":
                break;
            case "artifact":
                break;
            case "weapon":
                break;
            case "armor":
                break;
        }
        if (this.hp > 0 && this.isConsumable) {
            player.heal(this.hp);
        }
    }
}
