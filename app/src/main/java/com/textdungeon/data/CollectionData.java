package com.textdungeon.data;

public class CollectionData {
    String name, desc, imgId;
    boolean discovered;

    public CollectionData(String name, String desc, String imgId, boolean discovered) {
        this.name = name;
        this.desc = desc;
        this.imgId = imgId;
        this.discovered = discovered;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public String getImgId() {
        return imgId;
    }

    public boolean isDiscovered() {
        return discovered;
    }
}