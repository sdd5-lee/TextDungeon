package com.textdungeon.model;

import com.google.gson.Gson;

public class Item {
    private String name;
    private String id;

    private Item() {}
    public static Item createFromJson(String json) {
        return new Gson().fromJson(json, Item.class);
    }

    public String getName() { return name; }
    public String getId() { return id; }
}
