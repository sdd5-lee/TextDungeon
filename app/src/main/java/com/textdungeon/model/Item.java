package com.textdungeon.model;

import com.google.gson.Gson;
import com.textdungeon.player.Player;

public class Item {
    private String name;
    private String description;
    private String id;
    private String type;
    private String imgId;
    private int hp;
    private int atk;
    private int value;
    private int crit;
    private int magicDamage;
    private ItemGrade grade;
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

    public String getImgId() {
        return imgId;
    }

    /** 보이는 등급 (테두리·이름 색). 적혀 있지 않으면 일반 */
    public ItemGrade getGrade() {
        return grade != null ? grade : ItemGrade.COMMON;
    }
}
