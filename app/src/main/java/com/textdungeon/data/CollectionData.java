package com.textdungeon.data;

public class CollectionData {
    private String name, desc;
    private int imgResId;
    private boolean discovered;

    public CollectionData(String name, String desc, int imgResId, boolean discovered) {
        this.name = name;
        this.desc = desc;
        this.imgResId = imgResId;
        this.discovered = discovered;
    }

    public String getName() { return name; }
    public String getDesc() { return desc; }
    public int getImgResId() { return imgResId; }
    public boolean isDiscovered() { return discovered; }
}