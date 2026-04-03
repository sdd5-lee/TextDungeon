package com.textdungeon.model;

public class Stat {
    private int atk;//공격력
    private int strength, agility, health, wisdom;//힘,민첩,체력,지혜
    private int hp, maxhp;//체력, 최대체력
    private int critical_rate;//크리티컬 확률
    private int exp;
    private int maxexp;
    private int statpoint;

    public Stat(int strength, int agility, int health, int wisdom) {
        this.strength = strength;
        this.agility = agility;
        this.health = health;
        this.wisdom = wisdom;
        statpoint = 0;
        exp = 0 ;
        maxexp = 100;
        updateBattleStat();
    }

    public void updateBattleStat() {
        atk = strength;
        maxhp = health;
        hp = maxhp;
        critical_rate = agility;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public void setMaxHp(int maxhp) {
        this.maxhp = maxhp;
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
        return maxhp;
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
        return maxexp;
    }

    public int getStatpoint() {
        return statpoint;
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

    public void setStatpoint(int statpoint) {
        this.statpoint = statpoint;
    }
    public void addStatpoint(int statpoint) {
        this.statpoint += statpoint;
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
    }

    public void setExp(int exp) {
        this.exp = exp;
    }
    public void setMaxExp(int maxexp) {
        this.maxexp = maxexp;
    }
}
