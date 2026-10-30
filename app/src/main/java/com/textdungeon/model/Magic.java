package com.textdungeon.model;

import com.google.gson.Gson;
import com.textdungeon.player.Player;

public class Magic {
    String id;
    String name;
    private int count;
    private int maxCount;
    private int magicDamage;
    private int value;
    private String img;
    /** 계열 (없으면 기초) */
    private MagicSchool school;
    /** 먼저 배워야 하는 마법 id (없으면 바로 배울 수 있다) */
    private String requires;

    public Magic(){}

    /** 테스트용 */
    public Magic(String id, String name, MagicSchool school, String requires, int value) {
        this.id = id;
        this.name = name;
        this.school = school;
        this.requires = requires;
        this.value = value;
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

    public int getValue() {
        return value;
    }

    public String getImg() {
        return img;
    }

    public MagicSchool getSchool() {
        return school != null ? school : MagicSchool.BASIC;
    }

    public String getRequires() {
        return requires;
    }

    /** 지혜를 빼고 본 기본 피해 (세피로트의 나무 설명용) */
    public int getBaseDamage() {
        return magicDamage;
    }
}
