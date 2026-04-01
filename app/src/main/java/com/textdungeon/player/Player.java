package com.textdungeon.player;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private String name;
    private int level;
    Stat stat;
    Job job;
    Magic magic;
    private List<String> inventory;
    private static final int MAX_INV = 30;
    public Player(String name, Job job){
        this.name = name;
        this.level = 0;
        this.inventory = new ArrayList<>();
        this.job = job;
        this.stat = new Stat(job.strength, job.agility, job.health,job.wisdom );
        magic = new Magic(job.magic_count, stat.getWisdom());
    }
    public boolean pickUpItem(String item) {
        if (inventory.size() < MAX_INV) {
            inventory.add(item);
            return true;
        }
        return false;
    }

    public void setName(String name) {this.name = name;}
    public String getName() {return name;}
    public List<String> getInventory() { return inventory; }
    public void LevelUp(int level) { this.level += level; }


    public int attack(){

        return 0;
    }

    public void takeDamage(int damage) {
        stat.setHp(-damage);
        if (stat.getHp() < 0) stat.setHp(0);
    }
    public void heal(int heal) {
        stat.setHp(+heal);
        if (stat.getMaxHp() < stat.getHp()) stat.setHp(stat.getMaxHp());
    }
}
