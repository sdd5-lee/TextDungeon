package com.textdungeon.player;

import com.textdungeon.model.Item;
import com.textdungeon.model.Job;
import com.textdungeon.model.LearnedMagic;
import com.textdungeon.model.Magic;
import com.textdungeon.model.Stat;
import com.textdungeon.model.Trait;

public class Player {
    private String name;
    private int level;
    private final Stat stat;
    private final Job job;
    private String traitId;
    private final Inventory inventory;
    private final MagicScroll magicScroll;
    private final Equipment equipment;
    private int diceChane;

    public Player(String name, Job job){
        this.name = name;
        level = 1;
        diceChane = 1;
        inventory = new Inventory();
        equipment = new Equipment();
        magicScroll = new MagicScroll();
        this.job = job;
        this.traitId = job.trait.name();
        this.stat = new Stat(job.strength, job.agility, job.health,job.wisdom );

        stat.updateBattleStat(level);
    }

    public void levelUp() {
        while (stat.getExp() >= stat.getMaxExp()){
            int exp = stat.getExp();
            int maxExp = stat.getMaxExp();

            this.level++;

            stat.setExp(exp - maxExp);
            stat.setMaxExp(80 + this.level * 25);
            stat.addStatPoint(5);

            //레벨 반영
            stat.updateBattleStat(level);

            magicScroll.updateCounts(stat.getWisdom());
        }
    }


    // ****************** 인벤토리 ******************
    public void pickUpItem(Item item) {
        inventory.addItem(item);
    }
    public void consumablesItem(Item item){
        if(inventory.consumeItem(item.getId())){
            item.itemUse(this);
        }
    }

    // ****************** 장비창 ******************
    public void equipItem(Item item) {
        int oldEquipHp = equipment.getTotalHp();

        inventory.consumeItem(item.getId());
        Item old = equipment.equip(item);
        if (old != null) inventory.addItem(old);

        int newEquipHp = equipment.getTotalHp();

        int hpDiff = newEquipHp - oldEquipHp;
        applyHpChange(hpDiff);
    }
    public void equipArtifact(int index, Item item) {
        int oldEquipHp = equipment.getTotalHp();

        inventory.consumeItem(item.getId());
        Item old = equipment.equip(item,index);
        if (old != null) inventory.addItem(old);

        int newEquipHp = equipment.getTotalHp();

        int hpDiff = newEquipHp - oldEquipHp;
        applyHpChange(hpDiff);
    }
    public void unequipItem(String type, int slotIndex) {
        if (inventory.isFullItem()) {
            return;
        }

        int oldEquipHp = equipment.getTotalHp();

        Item old = equipment.unequip(type, slotIndex);

        if (old != null) {
            inventory.addItem(old);
        }

        int newEquipHp = equipment.getTotalHp();
        applyHpChange(newEquipHp - oldEquipHp);
    }
    public void applyHpChange(int hpDiff) {
        int currentHp = stat.getHp();
        int newHp = currentHp + hpDiff;
        int totalMax = getMaxHp();
        if (currentHp > 0 && newHp <= 0){
            newHp = 1;
        }if (newHp > totalMax) {
            newHp = totalMax;
        }
        stat.setHp(newHp);
    }

    // ******************배틀 이벤트에서 사용******************
    public void takeDamage(int damage) {
        int newHp = Math.max(0, stat.getHp() - damage);
        stat.setHp(newHp);
    }
    public void heal(int heal) {
        int newHp = Math.min(getMaxHp(), stat.getHp() + heal);
        stat.setHp(newHp);
    }
    public void refreshHp() {
        int max = getMaxHp();
        if (stat.getHp() > max) {
            stat.setHp(max);
        }
    }
    public int castMagic(Magic magic) {
        LearnedMagic lm = magicScroll.getMagic(magic.getId());
        if (lm != null && lm.use()) {
            int baseMagicDamage = magic.getMagicDamage(stat.getWisdom()) + equipment.getTotalMagicDamage();

            if (getTrait() != null) {
                baseMagicDamage = getTrait().modifyMagicDamage(this, baseMagicDamage);
            }
            return baseMagicDamage;
        }
        return 0;
    }

    public void useDice() {
        if (diceChane > 0) {
            diceChane--;
        }
    }

    //****************** 최종 공격력 체력 게터******************
    public int getFinalAtk() {
        int atk = stat.getAtk() + equipment.getTotalAtk();
        if (getTrait() != null) {
            atk = getTrait().modifyAtk(this,atk);
        }
        return atk;
    }
    public int getMaxHp() {
        int maxHp = stat.getMaxHp() + equipment.getTotalHp();
        if (getTrait() != null) maxHp = getTrait().modifyMaxHp(this, maxHp);
        return maxHp;
    }
    public int getTotalCrit() {
        int crit = stat.getCritical_rate() + equipment.getCrit();
        if (getTrait() != null) crit = getTrait().modifyCrit(this, crit);
        return crit;
    }
    // ****************** 특성 ******************
    public Trait getTrait() {
        if (traitId == null) return null;
        try {
            return Trait.valueOf(traitId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    //******************게터들******************

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
    public Stat getStat() {
        return stat;
    }
    public void setName(String name) {this.name = name;}
    public MagicScroll getMagicScroll() {
        return magicScroll;
    }
    public int getDiceChane() {
        return diceChane;
    }
    public void addDiceChane(int diceChane) {
        this.diceChane += diceChane;
    }
    public String getTraitId() { return traitId; }
    public void setTraitId(String traitId) { this.traitId = traitId; }

}
