package com.textdungeon.player;


import com.textdungeon.model.Item;
import com.textdungeon.model.Stat;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private String name;
    private int level;
    private Stat stat;
    private Job job;
    private Magic magic;
    private List<Item> inventory;
    private static final int MAX_INV = 30;
    public Player(String name, Job job){
        this.name = name;
        this.level = 0;
        this.inventory = new ArrayList<>();
        this.job = job;
        this.stat = new Stat(job.strength, job.agility, job.health,job.wisdom );
        magic = new Magic(job.magic_count, stat.getWisdom());
    }
    public boolean pickUpItem(Item item) {
        if (inventory.size() < MAX_INV) {
            inventory.add(item);
            return true;
        }
        return false;
    }

    public void setName(String name) {this.name = name;}
    public String getName() {return name;}
    public List<Item> getInventory() { return inventory; }
    public void levelUp() {
        while (stat.getExp() >= stat.getMaxExp()){
            int exp = stat.getExp();
            int maxexp = stat.getMaxExp();
            this.level++;
            stat.setExp(exp - maxexp);
            stat.setMaxExp((exp/2) + maxexp);
            stat.addStatpoint(5);
        }
    }

    public int getLevel() {
        return level;
    }
    public Job getJob() {
        return job;
    }
    public Magic getMagic() {
        return magic;
    }
    public Stat getStat() {
        return stat;
    }

    public int attack(){
        return stat.getAtk();
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
