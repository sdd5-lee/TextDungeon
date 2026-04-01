package com.textdungeon.player;

public class stat {
    private int strength = 5,agility = 5,health = 5,wisdom = 5;//힘,민첩,체력,지혜

    private int critical_rate = 0;
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
    public int getCritical_rate() {
        return critical_rate;
    }
    public void addCritical_rate(int critical_rate) {
        this.critical_rate = (int) (critical_rate*0.5);
        if(this.critical_rate > 101) this.critical_rate = 100;
    }
    public void gainStat(String type, int value) {
        switch(type) {
            case "힘": this.strength += value; break;
            case "민첩": this.agility += value; addCritical_rate(getAgility());break;
            case "체력": this.health += value; break;
            case "지혜": this.wisdom += value; break;
        }
    }
}
