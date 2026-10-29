package com.textdungeon.data;

public class CollectionData {
    private String name, desc;
    // 이미지 이름 (GameImages로 찾는다)
    private String imgName;
    private boolean discovered;

    public CollectionData(String name, String desc, String imgName, boolean discovered) {
        this.name = name;
        this.desc = desc;
        this.imgName = imgName;
        this.discovered = discovered;
    }

    public String getName() { return name; }
    public String getDesc() { return desc; }
    public String getImgName() { return imgName; }
    public boolean isDiscovered() { return discovered; }
}
