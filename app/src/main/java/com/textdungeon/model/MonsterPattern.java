package com.textdungeon.model;

/**
 * 몬스터 기술 하나. 평타 대신 쓰는 공격이다 (독·회복 같은 특수 효과는 없다).
 * 일반 몬스터는 하나, 보스는 둘을 가진다. 언제 쓰는지는 EnemyAi가 정한다.
 */
public class MonsterPattern {
    private String name;
    /** 한 번 때릴 때 공격력의 몇 % (150이면 1.5배) */
    private int power = 100;
    /** 몇 번 때리는지 */
    private int hits = 1;
    /** true면 한 턴 기운을 모았다가 다음 턴에 쓴다 (모으는 턴엔 공격하지 않는다) */
    private boolean charge;

    public MonsterPattern() {}

    /** 테스트용 */
    public MonsterPattern(String name, int power, int hits, boolean charge) {
        this.name = name;
        this.power = power;
        this.hits = hits;
        this.charge = charge;
    }

    public String getName() { return name; }
    public int getPower() { return power; }
    public int getHits() { return Math.max(1, hits); }
    public boolean isCharge() { return charge; }
}
