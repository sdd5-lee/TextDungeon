package com.textdungeon.player;

import com.textdungeon.model.Item;
import com.textdungeon.model.Job;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerTest {

    private Player player;

    private Item item(String id, String type, int atk, int hp, int crit) {
        String json = String.format(
            "{\"id\":\"%s\",\"name\":\"%s\",\"type\":\"%s\"," +
            "\"hp\":%d,\"atk\":%d,\"value\":0,\"crit\":%d,\"magicDamage\":0}",
            id, id, type, hp, atk, crit);
        return Item.createFromJson(json);
    }

    @Before
    public void setUp() {
        player = new Player("테스터", Job.WARRIOR);
    }

    // ── 초기 상태 ────────────────────────────────────────────

    @Test
    public void 초기_레벨은_1() {
        assertEquals(1, player.getLevel());
    }

    @Test
    public void 초기_diceChane는_1() {
        assertEquals(1, player.getDiceChance());
    }

    @Test
    public void 초기_직업은_생성자에서_넘긴_직업() {
        assertEquals(Job.WARRIOR, player.getJob());
    }

    @Test
    public void 초기_hp는_maxHp와_같음() {
        assertEquals(player.getMaxHp(), player.getStat().getHp());
    }

    // ── takeDamage ───────────────────────────────────────────

    @Test
    public void takeDamage_정상_감소() {
        int before = player.getStat().getHp();
        player.takeDamage(10);
        assertEquals(before - 10, player.getStat().getHp());
    }

    @Test
    public void takeDamage_hp가_0_미만이_되지_않음() {
        player.takeDamage(99999);
        assertEquals(0, player.getStat().getHp());
    }

    // ── heal ─────────────────────────────────────────────────

    @Test
    public void heal_정상_회복() {
        player.takeDamage(20);
        int before = player.getStat().getHp();
        player.heal(10);
        assertEquals(before + 10, player.getStat().getHp());
    }

    @Test
    public void heal_maxHp_초과하지_않음() {
        player.heal(99999);
        assertEquals(player.getMaxHp(), player.getStat().getHp());
    }

    // ── getFinalAtk / getMaxHp ───────────────────────────────

    @Test
    public void getFinalAtk_장비없을때_stat_atk와_동일() {
        assertEquals(player.getStat().getAtk(), player.getFinalAtk());
    }

    @Test
    public void getFinalAtk_무기장착후_증가() {
        int before = player.getFinalAtk();
        Item sword = item("sword_01", "weapon", 30, 0, 0);
        player.pickUpItem(sword);
        player.equipItem(sword);
        assertEquals(before + 30, player.getFinalAtk());
    }

    @Test
    public void getMaxHp_장비없을때_stat_maxHp와_동일() {
        assertEquals(player.getStat().getMaxHp(), player.getMaxHp());
    }

    @Test
    public void getMaxHp_방어구장착후_증가() {
        int before = player.getMaxHp();
        Item armor = item("armor_01", "armor", 0, 100, 0);
        player.pickUpItem(armor);
        player.equipItem(armor);
        assertEquals(before + 100, player.getMaxHp());
    }

    // ── getTotalCrit ──────────────────────────────────────────

    @Test
    public void getTotalCrit_장비크리가_포함됨() {
        int baseCrit = player.getStat().getCritical_rate();
        Item ring = item("ring_01", "artifact", 0, 0, 20);
        player.pickUpItem(ring);
        player.equipArtifact(0, ring);
        assertEquals(baseCrit + 20, player.getTotalCrit());
    }

    // ── applyHpChange ─────────────────────────────────────────

    @Test
    public void applyHpChange_hp가_양수일때_1_미만으로_내려가지_않음() {
        // 현재 hp > 0인 상태에서 엄청난 hp 감소 시도
        player.applyHpChange(-99999);
        assertTrue(player.getStat().getHp() >= 1);
    }

    @Test
    public void applyHpChange_maxHp_초과하지_않음() {
        player.applyHpChange(99999);
        assertEquals(player.getMaxHp(), player.getStat().getHp());
    }

    @Test
    public void applyHpChange_hp가_이미_0이면_1로_보정_안됨() {
        // hp가 0인 상태(사망)에서는 1 보정 없이 0 유지
        player.takeDamage(99999); // hp = 0
        player.applyHpChange(-10);
        // currentHp > 0 조건 실패 → newHp = -10이지만 maxHp 클램프 없이
        // 코드상 0 이하면 그대로 세팅됨 (사망 판정 유지)
        assertTrue(player.getStat().getHp() <= 0);
    }

    // ── equipItem / unequipItem ───────────────────────────────

    @Test
    public void equipItem_인벤토리에서_제거되고_장착됨() {
        Item sword = item("sword_01", "weapon", 10, 0, 0);
        player.pickUpItem(sword);
        player.equipItem(sword);
        assertFalse(player.getInventory().isItem("sword_01"));
        assertEquals(sword, player.getEquipment().getWeapon());
    }

    @Test
    public void equipItem_교체시_이전장비_인벤토리로_이동() {
        Item sword1 = item("sword_01", "weapon", 10, 0, 0);
        Item sword2 = item("sword_02", "weapon", 20, 0, 0);
        player.pickUpItem(sword1);
        player.equipItem(sword1);
        player.pickUpItem(sword2);
        player.equipItem(sword2);
        assertTrue(player.getInventory().isItem("sword_01"));
    }

    @Test
    public void unequipItem_장비_해제후_인벤토리로_이동() {
        Item sword = item("sword_01", "weapon", 10, 0, 0);
        player.pickUpItem(sword);
        player.equipItem(sword);
        player.unequipItem("weapon", 0);
        assertNull(player.getEquipment().getWeapon());
        assertTrue(player.getInventory().isItem("sword_01"));
    }

    @Test
    public void unequipItem_인벤토리_가득차면_해제_안됨() {
        // 인벤토리 30종 채우기
        for (int i = 0; i < 30; i++) {
            String json = String.format(
                "{\"id\":\"item_%d\",\"name\":\"아이템\",\"type\":\"consumables\"," +
                "\"hp\":0,\"atk\":0,\"value\":0,\"crit\":0,\"magicDamage\":0}", i);
            player.pickUpItem(Item.createFromJson(json));
        }
        Item sword = item("sword_01", "weapon", 10, 0, 0);
        // 인벤에 직접 넣지 않고 장착 상태로 만들기 (consumeItem 없이)
        player.getInventory().consumeItem("item_0"); // 하나 제거
        player.pickUpItem(sword);
        player.equipItem(sword);
        // 다시 30종 채움 → 해제 불가
        String json = "{\"id\":\"item_0\",\"name\":\"아이템\",\"type\":\"consumables\"," +
                "\"hp\":0,\"atk\":0,\"value\":0,\"crit\":0,\"magicDamage\":0}";
        player.pickUpItem(Item.createFromJson(json));

        player.unequipItem("weapon", 0);
        // 인벤 풀이면 해제 안 됨 → weapon 여전히 장착
        assertEquals(sword, player.getEquipment().getWeapon());
    }

    // ── diceChane ────────────────────────────────────────────

    @Test
    public void useDice_감소() {
        player.useDice();
        assertEquals(0, player.getDiceChance());
    }

    @Test
    public void useDice_0일때_추가감소_없음() {
        player.useDice(); // 1→0
        player.useDice(); // 0에서 시도
        assertEquals(0, player.getDiceChance());
    }

    @Test
    public void addDiceChane_증가() {
        player.addDiceChane(2);
        assertEquals(3, player.getDiceChance());
    }

    // ── levelUp ──────────────────────────────────────────────

    @Test
    public void levelUp_exp가_충분하면_레벨증가() {
        player.getStat().setExp(100); // maxExp = 100
        player.levelUp();
        assertEquals(2, player.getLevel());
    }

    @Test
    public void levelUp_exp_잔여분_이월() {
        player.getStat().setExp(150); // 100 초과 → 50 이월
        player.levelUp();
        assertEquals(50, player.getStat().getExp());
    }

    @Test
    public void levelUp_statPoint_5_증가() {
        player.getStat().setExp(100);
        int pointBefore = player.getStat().getStatPoint();
        player.levelUp();
        assertEquals(pointBefore + 5, player.getStat().getStatPoint());
    }

    @Test
    public void levelUp_연속_레벨업_가능() {
        // exp 대량 적립 → 여러 번 레벨업
        player.getStat().setExp(500);
        player.levelUp();
        assertTrue(player.getLevel() >= 3);
    }

    @Test
    public void levelUp_exp_부족하면_레벨_변화없음() {
        player.getStat().setExp(50); // maxExp = 100
        player.levelUp();
        assertEquals(1, player.getLevel());
    }

    // ── consumablesItem ──────────────────────────────────────

    @Test
    public void consumablesItem_사용후_hp_회복() {
        player.takeDamage(30);
        int hpBefore = player.getStat().getHp();
        String json = "{\"id\":\"potion_01\",\"name\":\"포션\",\"type\":\"consumables\"," +
                "\"hp\":20,\"atk\":0,\"value\":0,\"crit\":0,\"magicDamage\":0}";
        Item potion = Item.createFromJson(json);
        player.pickUpItem(potion);
        player.consumablesItem(potion);
        assertTrue(player.getStat().getHp() > hpBefore);
    }

    @Test
    public void consumablesItem_인벤토리에_없으면_사용_안됨() {
        int hpBefore = player.getStat().getHp();
        player.takeDamage(10);
        String json = "{\"id\":\"potion_01\",\"name\":\"포션\",\"type\":\"consumables\"," +
                "\"hp\":20,\"atk\":0,\"value\":0,\"crit\":0,\"magicDamage\":0}";
        Item potion = Item.createFromJson(json);
        // pickUpItem 없이 바로 사용 시도
        player.consumablesItem(potion);
        // hp 변화 없어야 함
        assertEquals(hpBefore - 10, player.getStat().getHp());
    }

    // ── refreshHp ────────────────────────────────────────────

    @Test
    public void refreshHp_hp가_maxHp_초과시_maxHp로_보정() {
        // stat.setHp()로 직접 초과값 삽입
        player.getStat().setHp(player.getMaxHp() + 50);
        player.refreshHp();
        assertEquals(player.getMaxHp(), player.getStat().getHp());
    }

    @Test
    public void refreshHp_hp가_maxHp_이하면_변화없음() {
        player.takeDamage(10);
        int hpBefore = player.getStat().getHp();
        player.refreshHp();
        assertEquals(hpBefore, player.getStat().getHp());
    }
}
