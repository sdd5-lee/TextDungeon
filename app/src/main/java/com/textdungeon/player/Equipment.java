package com.textdungeon.player;

import com.textdungeon.model.Item;

public class Equipment {
    private Item weapon;
    private Item armor;
    private Item consumables;
    private Item[] artifact = new Item[2];



    public Item equipWeapon(Item item){
        return swapWeapon(item);
    }
    public Item equipArmor(Item item){
        return swapArmor(item);
    }
    public Item equipConsumables(Item item){
        return swapConsumables(item);
    }
    public Item equipArtifact(Item item,int slot){
        if (slot < 0 || slot >= artifact.length) {
            return item;
        }
        return swapArtifact(item, slot);
    }

    public Item swapWeapon(Item newWeapon){
        Item item = this.weapon;
        this.weapon = newWeapon;
        newWeapon = item;
        return newWeapon;
    }
    public Item swapArmor(Item newArmor){
        Item item = this.armor;
        this.armor = newArmor;
        newArmor = item;
        return newArmor;
    }
    public Item swapConsumables(Item newConsumables){
        Item item = this.consumables;
        this.consumables = newConsumables;
        newConsumables = item;
        return newConsumables;
    }
    public Item swapArtifact(Item newArtifact, int slot){
        Item item = this.artifact[slot];
        this.artifact[slot] = newArtifact;
        newArtifact = item;
        return newArtifact;
    }
    public int getTotalAtk(){
        int totalAtk= 0;
        if (weapon != null){
            totalAtk+=weapon.getAtk();
        }
        if (armor != null){
            totalAtk+=armor.getAtk();
        }
        for (Item i:artifact) {
            if (i !=null){
                totalAtk += i.getAtk();
            }
        }
        return totalAtk;
    }
    public int getTotalHp(){
        int totalHp = 0;
        if (weapon != null){
            totalHp +=weapon.getHp();
        }
        if (armor != null){
            totalHp +=armor.getHp();
        }
        for (Item i:artifact) {
            if (i !=null){
                totalHp += i.getHp();
            }
        }

        return 0;
    }
    public Item getArmor() {
        return armor;
    }
    public Item getConsumables() {
        return consumables;
    }

    public Item getWeapon() {
        return weapon;
    }

    public Item[] getArtifact() {
        return artifact;
    }
}
