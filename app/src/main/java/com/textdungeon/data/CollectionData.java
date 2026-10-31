package com.textdungeon.data;

import com.textdungeon.model.ItemGrade;

public class CollectionData {
    private String name;
    private CharSequence desc;
    // 이미지 이름 (GameImages로 찾는다)
    private String imgName;
    private boolean discovered;
    // 아이템만: 칸 틀과 이름 색 (몬스터·이벤트는 null)
    private ItemGrade grade;

    public CollectionData(String name, CharSequence desc, String imgName, boolean discovered) {
        this(name, desc, imgName, discovered, null);
    }

    public CollectionData(String name, CharSequence desc, String imgName, boolean discovered, ItemGrade grade) {
        this.name = name;
        this.desc = desc;
        this.imgName = imgName;
        this.discovered = discovered;
        this.grade = grade;
    }

    public String getName() { return name; }
    public CharSequence getDesc() { return desc; }
    public String getImgName() { return imgName; }
    public boolean isDiscovered() { return discovered; }
    public ItemGrade getGrade() { return grade; }

    /** 몬스터·이벤트 그림은 가로로 길어서 칸을 채우고 가운데를 보여 준다 (아이템은 정사각형이라 통째로) */
    public boolean fillsFrame() { return grade == null; }
}
