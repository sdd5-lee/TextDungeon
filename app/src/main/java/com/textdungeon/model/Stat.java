package com.textdungeon.model;

public class Stat {
    private int atk;//공격력
    private int strength, agility, health, wisdom;//힘,민첩,체력,지혜
    private int hp, maxHp;//체력, 최대체력
    private int critical_rate;//크리티컬 확률
    private int exp;
    private int maxExp;
    private int statPoint;

    private int gold;
    public Stat(int strength, int agility, int health, int wisdom) {
        this.strength = strength;
        this.agility = agility;
        this.health = health;
        this.wisdom = wisdom;

        this.gold = 100;

        statPoint = 0;
        exp = 0 ;
        maxExp = 100;
        updateBattleStat(0);
        hp = maxHp;
    }

    public void updateBattleStat(int level) {
        int oldMaxHp = this.maxHp;

        int baseAtk = strength * 2;
        int baseMaxHp = health * 10;
        int baseCrit = agility;

        this.atk = baseAtk + level;
        this.maxHp = baseMaxHp + level * 5;
        this.critical_rate = Math.min(baseCrit + level, 100);

        if (oldMaxHp > 0 && this.maxHp > oldMaxHp) {
            int diff = this.maxHp - oldMaxHp;
            this.hp += diff;
        }
        if (this.hp > this.maxHp) {
            this.hp = this.maxHp;
        }
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public void setMaxHp(int maxHp) {
        this.maxHp = maxHp;
        this.hp = maxHp;
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

    public void setStatPoint(int statPoint) {
        this.statPoint = statPoint;
    }
    public void addStatPoint(int statPoint) {
        this.statPoint += statPoint;
    }

    public void addStrength(int point){
        strength += point;
    }
    public void addAgility(int point){
        agility += point;
    }
    public void addHealth(int point){
        health += point;
    }
    public void addWisdom(int point){
        wisdom += point;
    }

    public int getGold() {
        return gold;
    }
    public void setGold(int gold) {
        this.gold = gold;
    }
    public void addGold(int amount) {
        this.gold += amount;
        if (this.gold < 0) this.gold = 0;
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
            // 보상의 데미지/회복은 Reward.apply()에서 Player.takeDamage/heal로 처리된다 (장비 HP 포함).
            // 아래는 직접 호출됐을 때를 위한 안전한 처리 (예전 식은 HP를 value로 덮어쓰거나 두 배로 만들었음)
            case "데미지":
                this.hp = Math.max(0, this.hp - Math.abs(value));
                break;
            case "회복":
                this.hp = Math.min(this.maxHp, this.hp + Math.abs(value));
                break;
            case "골드":
                this.addGold(value);
                break;
        }
    }

    private void addHp(int value) {
        hp += value;
    }

    public void gainStatExp(int value,int expBonus) {
        this.exp += value * expBonus;
    }

    public void setExp(int exp) {
        this.exp = exp;
    }
    public void setMaxExp(int maxExp) {
        this.maxExp = maxExp;
    }

}