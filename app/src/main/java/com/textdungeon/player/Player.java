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
    private int diceChance;
    /** 이번 판에서 처치한 몬스터 수 (정산용). 누적 처치 수는 UserRecord에 따로 있다. */
    private int runKillCount;

    public Player(String name, Job job){
        this.name = name;
        level = 1;
        diceChance = 1;
        inventory = new Inventory();
        equipment = new Equipment();
        magicScroll = new MagicScroll();
        this.job = job;
        this.traitId = job.trait.name();
        this.stat = new Stat(job.strength, job.agility, job.health,job.wisdom );

        stat.updateBattleStat(level);
        stat.setHp(getMaxHp());
    }

    public void levelUp() {
        while (stat.getExp() >= stat.getMaxExp()){
            int exp = stat.getExp();
            int maxExp = stat.getMaxExp();

            this.level++;

            stat.setExp(exp - maxExp);
            stat.setMaxExp(80 + this.level * 25);
            stat.addStatPoint(5);

            int equipHp = equipment.getTotalHp();
            stat.setHp(Math.max(0, stat.getHp() - equipHp));

            stat.updateBattleStat(level);

            stat.setHp(stat.getHp() + equipHp);
            magicScroll.updateCounts(stat.getWisdom());
        }
        refreshHp();
    }

    /**
     * 힘/민첩/체력/지혜가 레벨업 없이 바뀌었을 때(이벤트 보상, 스탯 포인트 분배) 호출.
     * 공격력·최대HP·치명타·마법 최대 횟수를 다시 계산한다.
     * levelUp()과 달리 남은 마법 횟수를 다시 채우지 않는다.
     *
     * @param previousWisdom 스탯이 바뀌기 전의 지혜 (마법 횟수를 변화량만큼만 조정하기 위해 필요)
     */
    public void recalculateStats(int previousWisdom) {
        // updateBattleStat()이 장비 HP를 모르고 HP를 잘라내므로, 장비 HP를 잠시 빼고 계산한다 (levelUp과 같은 방식)
        int equipHp = equipment.getTotalHp();
        stat.setHp(Math.max(0, stat.getHp() - equipHp));
        stat.updateBattleStat(level);
        stat.setHp(stat.getHp() + equipHp);

        magicScroll.adjustForWisdomChange(previousWisdom, stat.getWisdom());
        refreshHp();
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
    /**
     * 장비를 교체할 때 빠지는 장비를 가방에 넣을 자리가 있는지.
     * 장착하려는 아이템이 마지막 1개면 그 칸이 비므로 자리가 생긴다.
     */
    private boolean hasRoomForSwap(Item incoming, Item outgoing) {
        if (outgoing == null) return true;
        if (inventory.canAdd(outgoing.getId())) return true;
        return inventory.getCount(incoming.getId()) == 1;
    }

    /** @return 장착 성공 여부. 가방에 빠진 장비를 넣을 자리가 없거나 장착할 수 없는 종류면 false (아이템은 그대로 유지) */
    public boolean equipItem(Item item) {
        Item current;
        if ("weapon".equals(item.getType())) current = equipment.getWeapon();
        else if ("armor".equals(item.getType())) current = equipment.getArmor();
        else return false; // 무기/방어구가 아니면 이 메서드로 장착 불가 (예전엔 가방에서만 빠지고 사라졌음)
        if (!hasRoomForSwap(item, current)) return false;

        int oldEquipHp = equipment.getTotalHp();

        inventory.consumeItem(item.getId());
        Item old = equipment.equip(item);
        if (old != null) inventory.addItem(old);

        int newEquipHp = equipment.getTotalHp();

        int hpDiff = newEquipHp - oldEquipHp;
        applyHpChange(hpDiff);
        return true;
    }
    /** @return 장착 성공 여부 (equipItem과 동일한 규칙) */
    public boolean equipArtifact(int index, Item item) {
        Item[] slots = equipment.getArtifact();
        if (index < 0 || index >= slots.length) return false;
        if (!hasRoomForSwap(item, slots[index])) return false;

        int oldEquipHp = equipment.getTotalHp();

        inventory.consumeItem(item.getId());
        Item old = equipment.equip(item,index);
        if (old != null) inventory.addItem(old);

        int newEquipHp = equipment.getTotalHp();

        int hpDiff = newEquipHp - oldEquipHp;
        applyHpChange(hpDiff);
        return true;
    }
    /** @return 해제 성공 여부 */
    public boolean unequipItem(String type, int slotIndex) {
        Item target;
        switch (type.toLowerCase()) {
            case "weapon": target = equipment.getWeapon(); break;
            case "armor": target = equipment.getArmor(); break;
            case "artifact":
                Item[] slots = equipment.getArtifact();
                target = (slotIndex >= 0 && slotIndex < slots.length) ? slots[slotIndex] : null;
                break;
            default: target = null;
        }
        if (target == null || !inventory.canAdd(target.getId())) {
            return false;
        }

        int oldEquipHp = equipment.getTotalHp();

        Item old = equipment.unequip(type, slotIndex);

        if (old != null) {
            inventory.addItem(old);
        }

        int newEquipHp = equipment.getTotalHp();
        applyHpChange(newEquipHp - oldEquipHp);
        return true;
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
        int maxHp = getMaxHp();
        if (stat.getHp() > maxHp) {
            stat.setHp(maxHp);
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
        if (diceChance > 0) {
            diceChance--;
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
        if (getTrait() != null){
            maxHp = getTrait().modifyMaxHp(this, maxHp);
        }
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
    public int getDiceChance() {
        return diceChance;
    }
    public void addDiceChane(int diceChane) {
        this.diceChance += diceChane;
    }
    public String getTraitId() { return traitId; }
    public void addRunKill() { runKillCount++; }
    public int getRunKillCount() { return runKillCount; }
    public void setTraitId(String traitId) { this.traitId = traitId; }

}