package com.textdungeon.model;

import com.google.gson.Gson;
import com.textdungeon.player.Player;

public class Item {
    private String name;
    private String description;
    private String id;
    private String type;
    private int hp;
    private int atk;
    private int value;
    private int crit;
    private int magicDamage;
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
            player.heal(this.hp);
            return true;
        }
        return false;
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public int getMagicDamage() {
        return magicDamage;
    }
    public int getCrit() {
        return crit;
    }
}
