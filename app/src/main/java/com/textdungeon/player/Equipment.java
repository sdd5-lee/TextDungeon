package com.textdungeon.player;

import com.textdungeon.model.Item;
import com.textdungeon.model.Monster;

public class Equipment {
    private Item weapon;
    private Item armor;
    private Item consumables;
    private Item[] artifact = new Item[2];


    public Item equip(Item newItem, int slot) {
        if (newItem == null) return null;

        Item oldItem = null;

        switch (newItem.getType()) {
            case "weapon":
                oldItem = weapon;
                weapon = newItem;
                break;

            case "armor":
                oldItem = armor;
                armor = newItem;
                break;

            case "consumables":
                oldItem = consumables;
                consumables = newItem;
                break;

            case "artifact":
                if (slot >= 0 && slot < artifact.length) {
                    oldItem = artifact[slot];
                    artifact[slot] = newItem;
                } else {
                    return newItem;
                }
                break;
        }
        return oldItem;
    }
    public int getTotalAtk(){
        int totalAtk= 0;
        Item [] items = {weapon,armor,artifact[0],artifact[1]};
        for (Item i: items) {
            if (i != null) {
                totalAtk += i.getAtk();
            }
        }
        return totalAtk;
    }
    public int getTotalHp(){
        int totalHp= 0;
        Item [] items = {weapon,armor,artifact[0],artifact[1]};
        for (Item i: items) {
            if (i != null) {
                totalHp += i.getHp();
            }
        }
        return totalHp;
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
