package com.textdungeon.player;

import android.content.Context;

import com.textdungeon.data.DataControlTower;
import com.textdungeon.model.Item;
import com.textdungeon.model.Job;
import com.textdungeon.model.LearnedMagic;
import com.textdungeon.model.Magic;
import com.textdungeon.model.Stat;

public class Player {
    private String name;
    private int level;
    private Stat stat;
    private Job job;
    private Inventory inventory;
    private MagicScroll magicScroll;
    private Equipment equipment;
    private int money;

    public Player(String name, Job job){
        this.name = name;
        level = 1;
        money = 0;
        inventory = new Inventory();
        equipment = new Equipment();
        magicScroll = new MagicScroll();
        this.job = job;
        this.stat = new Stat(job.strength, job.agility, job.health,job.wisdom );

        stat.updateBattleStat(level);
    }

    public void levelUp() {
        while (stat.getExp() >= stat.getMaxExp()){
            int exp = stat.getExp();
            int maxExp = stat.getMaxExp();

            this.level++;

            stat.setExp(exp - maxExp);
            stat.setMaxExp(maxExp + maxExp/2);
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

    // Player.java 내부
    public void consumablesItem(Item item){
        // 인벤토리에서 차감 성공했다면
        if(inventory.consumeItem(item.getId())){
            // 아이템아, 나(this)한테 너의 효과를 발동시켜라!
            item.itemUse(this);
        }
    }

    // ****************** 장비창 ******************
    public void equipItem(Item item) {
        int oldEquipHp = equipment.getTotalHp();

        inventory.consumeItem(item.getId());
        Item old = equipment.equip(item,0);
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
    public int castMagic(String magicId, Context context) {
        LearnedMagic lm = magicScroll.getMagic(magicId);

        if (lm != null && lm.use()) {
            Magic magic = DataControlTower.getInstance(context).getMagicManager().spawn(magicId);

            return magic.getMagicDamage(stat.getWisdom());
        }

        return 0;
    }

    //****************** 최종 공격력 체력 게터******************
    public int getFinalAtk() {
        return stat.getAtk() + equipment.getTotalAtk();
    }

    public int getMaxHp(){
        return stat.getMaxHp() + equipment.getTotalHp();
    }

    //******************게터들******************

    public int getMoney() {
        return money;
    }
    public void addMoney(int money){
        this.money += money;
    }
    public void setMoney(int money) {
        this.money = money;
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
    public Stat getStat() {
        return stat;
    }
    public void setName(String name) {this.name = name;}
    public MagicScroll getMagicScroll() {
        return magicScroll;
    }

}
