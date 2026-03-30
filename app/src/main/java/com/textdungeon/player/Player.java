package com.textdungeon.player;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private String name;
    private int level;
    private int hp,maxhp;
    private int strength = 5,agility = 5,health = 5,wisdom = 5;//힘,민첩,체력,지혜
    private int magic_count;
    private int critical_rate = 0;
    private List<String> inventory;
    private static final int MAX_INV = 30;
    public Player(String name){
        this.name = name;
        this.level = 0;
        this.inventory = new ArrayList<>();
    }
    public boolean pickUpItem(String item) {
        if (inventory.size() < MAX_INV) {
            inventory.add(item);
            return true;
        }
        return false;
    }

    public void setName(String name) {
        this.name = name;
    }
    public void setHp(int hp) {
        this.hp = hp;
    }
    public void setMaxhp(int maxhp) {
        this.maxhp = maxhp;
        this.hp = maxhp;
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
    public void setMagic_count(int magic_count) {
        this.magic_count = magic_count;
    }
    public void addCritical_rate(int critical_rate) {
        this.critical_rate = (int) (critical_rate*0.5);
        if(this.critical_rate > 101) this.critical_rate = 100;
    }

    public String getName() {
        return name;
    }
    public int getHp() {
        return hp;
    }
    public int getMaxhp() {
        return maxhp;
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
    public int getMagic_count() {
        return magic_count;
    }
    public int getCritical_rate() {
        return critical_rate;
    }
    public List<String> getInventory() { return inventory; }
    public void addLevel(int level) {
        this.level += level;
    }

    public void gainStat(String type, int value) {
        switch(type) {
            case "힘": this.strength += value; break;
            case "민첩": this.agility += value; addCritical_rate(getAgility());break;
            case "체력": this.health += value; break;
            case "지혜": this.wisdom += value; break;
        }
    }
    public void takeDamage(int damage) {
        this.hp -= damage;
        if (this.hp < 0) this.hp = 0;
    }

    public void heal(int heal) {
        this.hp += heal;
        if (this.maxhp < this.hp) this.hp = maxhp;
    }

}
