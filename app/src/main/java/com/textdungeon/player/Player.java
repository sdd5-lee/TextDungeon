package com.textdungeon.player;


import com.textdungeon.model.Item;
import com.textdungeon.model.Stat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Player {
    private String name;
    private int level;
    private Stat stat;
    private Job job;
    private Magic magic;
    private Map<String,Integer> inventory;
    private Map<String,Item> equipment;
    private static final int MAX_INV = 30;
    private static final int MAX_equipment = 3;
    public Player(String name, Job job){
        this.name = name;
        this.level = 0;
        this.inventory = new HashMap<>();
        this.equipment = new HashMap<>();
        this.job = job;
        this.stat = new Stat(job.strength, job.agility, job.health,job.wisdom );
        magic = new Magic(job.magic_count, stat.getWisdom());
    }
    public boolean pickUpItem(Item item) {
        if(item == null) {return false;}
        String itemName = item.getName();
        if (inventory.containsKey(itemName)){
            inventory.put(itemName, inventory.get(itemName)+1);
            return true;
        }
        if (inventory.size() < MAX_INV) {
            inventory.put(itemName,1);
            return true;
        }
        return false;
    }
    public void useItem(Item item){
        if(item == null) {return;}
        String itemName = item.getName();
        if (inventory.containsKey(itemName)){
            item.itemUse(this);
        }
    }
    public void levelUp() {
        while (stat.getExp() >= stat.getMaxExp()){
            int exp = stat.getExp();
            int maxexp = stat.getMaxExp();
            this.level++;
            stat.setExp(exp - maxexp);
            stat.setMaxExp((exp/2) + maxexp);
            stat.addStatPoint(5);
        }
    }
    public void armorEquip(Item item){
        if(item == null) {return;}
        String itemName = item.getName();
        if (isEquip(itemName)) {
            equipment.put(itemName,item);
        }
    }
    public void armorNotEquip(Item item){
        if(item == null) {return;}
        String itemName = item.getName();
        if (equipment == null){
            return;
        }
        if (equipment.containsKey(itemName)) {
            equipment.remove(itemName);
            pickUpItem(item);
        }
    }
    public boolean isEquip(String itemName){
        if (equipment.containsKey(itemName)){
            return false;
        }if(equipment.size() < MAX_equipment){
            return true;
        }
        return false;
    }

    public String getName() {return name;}
    public Map<String,Integer> getInventory() { return inventory; }
    public Map<String, Item> getEquipment() {
        return equipment;
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


    public void setName(String name) {this.name = name;}
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
