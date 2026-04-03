package com.textdungeon.player;

public enum Job {
    //스텟 총량은 30으로 설정 매직 카운트는 지혜 / 2
    WARRIOR("전사", 12, 6, 12,0,0, "불굴", true),
    MAGE("마법사", 4, 6, 8,12,6, "마력폭주", true),
    ROGUE("도적", 6, 12, 6,4,2, "생존본능", true),
    ARCHER("궁수", 8, 12, 8,2,1, "명사수", true),

    //잠금 직업 추가
    HERO("용사", 15,15,15,15,7,"용사의의지",false);

    public final String name;
    public final int strength,agility,health,wisdom;//힘,민첩,체력,지혜
    public final int magic_count;

    //직업별 특성 추가
    public final String traitName;
    //해금 여부 확인
    public final boolean defaultUnlokced;
    Job(String name, int strength, int agility, int health, int wisdom, int magic_count, String traitName, boolean defaultUnlokced){
        this.name = name;
        this.strength = strength;
        this.agility = agility;
        this.health = health;
        this.wisdom = wisdom;
        this.magic_count = magic_count;

        //특성
        this.traitName = traitName;
        //해금
        this.defaultUnlokced = defaultUnlokced;
    }
}
