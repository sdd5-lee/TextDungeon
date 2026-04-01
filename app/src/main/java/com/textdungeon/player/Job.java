package com.textdungeon.player;

public enum Job {
    //스텟 총량은 30으로 설정 매직 카운트는 지혜 / 2
    WARRIOR("전사", 12, 6, 12,0,0),
    MAGE("마법사", 4, 4, 4,12,6),
    ROGUE("도적", 6, 12, 6,4,2),
    ARCHER("궁수", 8, 12, 8,2,1);

    public final String name;
    public final int strength,agility,health,wisdom;//힘,민첩,체력,지혜
    public final int magic_count;
    Job(String name, int strength, int agility, int health, int wisdom, int magic_count){
        this.name = name;
        this.strength = strength;
        this.agility = agility;
        this.health = health;
        this.wisdom = wisdom;
        this.magic_count = magic_count;
    }
}
