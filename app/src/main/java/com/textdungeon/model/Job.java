package com.textdungeon.model;

public enum Job {
    // 스텟 총량은 30으로 설정, 매직 카운트는 지혜 / 2
    WARRIOR("전사", 11, 6, 11, 2, Trait.INDOMITABLE, true,
            "강력한 근력과 체력을 바탕으로 최전선에서 적과 맞서는 든든한 전사입니다.", 0, "job_warrior"),
    MAGE("마법사", 4, 6, 8, 12, Trait.MANA_SURGE, true,
            "높은 지혜를 바탕으로 파괴적인 마법을 구사하여 적을 섬멸하는 마법사입니다.", 0, "job_mage"),
    ROGUE("도적", 8, 12, 8, 2, Trait.SURVIVAL_INSTINCT, true,
            "빠른 몸놀림으로 치명적인 타격을 입히고 위기에서 쉽게 벗어나는 도적입니다.", 0, "job_rogue"),
    ARCHER("궁수", 10, 12, 6, 2, Trait.SHARPSHOOTER, true,
            "뛰어난 민첩성을 활용해 먼 거리에서 적을 정확하게 저격하는 궁수입니다.", 0, "job_archer"),

    // 잠금 직업 해금
    KNIGHT("기사", 10, 4, 14, 2, Trait.IRON_SHIELD, false,
            "압도적인 방어력과 체력을 지닌 수호자입니다.", 1000, "job_knight"),
    MONK("수도사", 12, 10, 6, 2, Trait.NIRVANA, false,
            "혹독한 수련으로 단련된 육체를 무기로 삼아 빠른 연타를 날리는 무투가입니다.", 1500, "job_monk"),
    PALADIN("성기사", 4, 6, 8, 12, Trait.DIVINE_BLESSING, false,
            "신성한 힘을 빌려 전투하는 성기사입니다.", 2000, "job_paladin"),
    WARLOCK("전투 마법사", 8, 6, 8, 8, Trait.DARK_PACT, false,
            "무력과 마법을 동시에 다루며, 전투 상황에 유연하게 대처하는 마검사입니다.", 2500, "job_warlock"),

    HERO("용사", 14, 14, 14, 12, Trait.HEROES_WILL, false,
            "모든 능력치가 뛰어나며 세상을 구원할 운명을 지닌 전설의 용사입니다.", 5000, "job_hero");

    public final String name;
    public final int strength, agility, health, wisdom;

    // 직업별 특성
    public final Trait trait;
    // 해금 여부 확인
    public final boolean defaultUnlocked;
    public final String description;
    public final int price;
    public final String img;

    Job(String name, int strength, int agility, int health, int wisdom, Trait trait, boolean defaultUnlocked, String description, int price, String img) {
        this.name = name;
        this.strength = strength;
        this.agility = agility;
        this.health = health;
        this.wisdom = wisdom;

        this.trait = trait;
        this.defaultUnlocked = defaultUnlocked;
        this.description = description;
        this.price = price;
        this.img = img;
    }
}