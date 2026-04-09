package com.textdungeon.model;

import com.google.gson.Gson;
import com.textdungeon.player.Player;

public class Magic {
    String id;
    String name;
    private int count;
    private int maxCount;
    private int magicDamage;

    public Magic(){}
    public static Magic createFromJson(String json) {
        return new Gson().fromJson(json, Magic.class);
    }

    public int getMaxCount() {
        return maxCount;
    }

    public int getCount() {return count;}
    public int getMagicDamage(int wisdom) {
        return magicDamage + (wisdom / 2);
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

}
