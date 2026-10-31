package com.textdungeon.model;

import java.util.Collections;
import java.util.List;

public class Monster {
    private String name;
    private String id;
    private int hp;
    private int maxHp;
    private int attack;
    private String imgId;
    private String description;
    /** 기술. 일반 몬스터는 1개, 보스는 2개 (없으면 평타만) */
    private List<MonsterPattern> patterns;
    private Monster() {}

    public void takeDamage(int damage) {
        this.hp -= damage;
        if (this.hp < 0) this.hp = 0;
    }

    public boolean isDead() {
        return this.hp <= 0;
    }

    public String getName() { return name; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public int getAttack() { return attack; }
    public String getImgId() { return imgId; }
    public String getId() { return id; }

    public String getDescription() {
        return description;
    }

    public List<MonsterPattern> getPatterns() {
        return patterns != null ? patterns : Collections.emptyList();
    }

    /** 기술이 둘이면 보스 (체력이 절반 이하가 되면 두 번째 기술을 쓴다) */
    public boolean isBoss() {
        return getPatterns().size() >= 2;
    }

    /** 체력·공격력을 percent%로 바꾼 사본 (시련의 신 결투). 원본은 그대로 둔다 */
    public Monster scaled(int percent) {
        Monster m = new Monster();
        m.name = name;
        m.id = id;
        m.imgId = imgId;
        m.description = description;
        m.patterns = patterns;
        m.hp = Math.max(1, hp * percent / 100);
        m.maxHp = Math.max(1, maxHp * percent / 100);
        m.attack = attack * percent / 100;
        return m;
    }
}