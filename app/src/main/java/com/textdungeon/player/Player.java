package com.textdungeon.player;


import com.textdungeon.model.Item;
import com.textdungeon.model.Stat;

public class Player {
    private String name;
    private int level;
    private Stat stat;
    private Job job;
    private Magic magic;
    private Inventory inventory;
    private Equipment equipment;
    public Player(String name, Job job){
        this.name = name;
        this.level = 0;
        this.inventory = new Inventory();
        this.equipment = new Equipment();
        this.job = job;
        this.stat = new Stat(job.strength, job.agility, job.health,job.wisdom );
        magic = new Magic(job.magic_count, stat.getWisdom());
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
    public void pickUpItem(Item item) {
        inventory.addItem(item);
    }
    public void equipWeapon(Item item) {
        inventory.removeItem(item.getName());
        Item old = equipment.swapWeapon(item);
        if (old != null) inventory.addItem(old);
    }
    public void equipArmor(Item item) {
        inventory.removeItem(item.getName());
        Item old = equipment.swapArmor(item);
        if (old != null) inventory.addItem(old);
    }
    public void equipConsumables(Item item) {
        inventory.removeItem(item.getName());
        Item old = equipment.swapConsumables(item);
        if (old != null) inventory.addItem(old);
    }
    public void equipArtifact(int index, Item item) {
        inventory.removeItem(item.getName());
        Item old = equipment.swapArtifact(item,index);
        if (old != null) inventory.addItem(old);
    }
    public int getFinalAtk(){
        return stat.getAtk() + equipment.getTotalAtk();
    }
    public int getFinalHp(){
        return stat.getMaxHp() + equipment.getTotalHp();
    }

    public void useItem(Item item){
        if(item == null) {return;}
        String itemName = item.getName();
        if (inventory.getItemMap().containsKey(itemName)){
            item.itemUse(this);
        }
    }


    public String getName() {return name;}
    public Inventory getInventory() { return inventory; }
    public Equipment getEquipment() {
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
