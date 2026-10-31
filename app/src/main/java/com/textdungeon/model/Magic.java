package com.textdungeon.model;

public class Magic {
    String id;
    String name;
    /** 지혜를 빼고 본 기본 피해 */
    private int magicDamage;
    /** 지혜의 몇 %를 피해에 더하는지 (70이면 지혜 10당 +7) */
    private int wisdomPercent;
    /** 쓰고 나서 다시 쓰기까지 쉬는 내 턴 수 (싸움마다 처음엔 모두 준비돼 있다) */
    private int cooldown;
    /** 배우는 데 필요한 지혜 */
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

    /** 기본 피해 + 지혜 × 비율 */
    public int getMagicDamage(int wisdom) {
        return magicDamage + wisdom * wisdomPercent / 100;
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

    public int getWisdomPercent() {
        return wisdomPercent;
    }

    public int getCooldown() {
        return cooldown;
    }
}
