package com.textdungeon.player;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private String name;
    private int level;
    private int hp,maxhp;
    private int magic_count;
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

    public void setName(String name) {this.name = name;}
    public void setHp(int hp) {this.hp = hp;}
    public void setMaxhp(int maxhp) {
        this.maxhp = maxhp;
        this.hp = maxhp;
    }
    public void setMagic_count(int magic_count) {this.magic_count = magic_count;}

    public String getName() {return name;}
    public int getHp() {return hp;}
    public int getMaxhp() {return maxhp;}
    public int getMagic_count() {return magic_count;}
    public List<String> getInventory() { return inventory; }
    public void addLevel(int level) {
        this.level += level;
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
