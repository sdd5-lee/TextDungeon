package com.textdungeon.player;

import com.textdungeon.model.Item;

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
    public Item equip(Item newItem) {
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
        }
        return oldItem;
    }
    public Item unequip(String type, int slot) {
        Item oldItem = null;
        switch (type.toLowerCase()) {
            case "weapon":
                oldItem = weapon;
                weapon = null;
                break;
            case "armor":
                oldItem = armor;
                armor = null;
                break;
            case "artifact":
                if (slot >= 0 && slot < artifact.length) {
                    oldItem = artifact[slot];
                    artifact[slot] = null;
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
    public int getCrit(){
        int Crit = 0;
        Item [] items = {weapon,armor,artifact[0],artifact[1]};
        for (Item i: items) {
            if (i != null) {
                Crit += i.getCrit();
            }
        }
        return Crit;
    }
    public int getTotalMagicDamage(){
        int totalMagicDamage = 0;
        Item [] items = {weapon,armor,artifact[0],artifact[1]};
        for (Item i: items) {
            if (i != null) {
                totalMagicDamage += i.getMagicDamage();
            }
        }
        return totalMagicDamage;
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
