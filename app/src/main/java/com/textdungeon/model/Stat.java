package com.textdungeon.model;

public class Stat {
    private int atk;//공격력
    private int strength, agility, health, wisdom;//힘,민첩,체력,지혜
    private int hp, maxHp;//체력, 최대체력
    private int critical_rate;//크리티컬 확률
    private int exp;
    private int maxExp;
    private int statPoint;

    public Stat(int strength, int agility, int health, int wisdom) {
        this.strength = strength;
        this.agility = agility;
        this.health = health;
        this.wisdom = wisdom;
        statPoint = 0;
        exp = 0 ;
        maxExp = 100;
        updateBattleStat(0);
        hp = maxHp;
    }

    public void updateBattleStat(int level) {
        int baseAtk = strength * 2;
        int baseMaxHp = health * 10;
        int baseCrit = agility;

        atk = baseAtk + level;
        maxHp = baseMaxHp + level * 5;
        critical_rate = Math.min(baseCrit + level, 100);

        if (hp > maxHp) hp = maxHp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public void setMaxHp(int maxhp) {
        this.maxHp = maxhp;
        this.hp = maxhp;
    }

    public int getStrength() {
        return strength;
    }

    public int getAgility() {
        return agility;
    }

    public int getHealth() {
        return health;
    }

    public int getWisdom() {
        return wisdom;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getCritical_rate() {
        return critical_rate;
    }

    public int getExp() {
        return exp;
    }

    public int getAtk() {
        return atk;
    }

    public int getMaxExp() {
        return maxExp;
    }

    public int getStatPoint() {
        return statPoint;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }

    public void setAgility(int agility) {
        this.agility = agility;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public void setWisdom(int wisdom) {
        this.wisdom = wisdom;
    }

    public void setStatPoint(int statpoint) {
        this.statPoint = statpoint;
    }
    public void addStatPoint(int statpoint) {
        this.statPoint += statpoint;
    }
    public void subHp(int damage){
        hp -= damage;
    }

    public void gainStat(String type, int value) {
        switch (type) {
            case "힘":
                this.strength += value;
                break;
            case "민첩":
                this.agility += value;
                break;
            case "체력":
                this.health += value;
                break;
            case "지혜":
                this.wisdom += value;
                break;
            case "경험치":
                this.exp += value;
                break;
        }
        updateBattleStat(1);
    }

    public void setExp(int exp) {
        this.exp = exp;
    }
    public void setMaxExp(int maxExp) {
        this.maxExp = maxExp;
    }
}
