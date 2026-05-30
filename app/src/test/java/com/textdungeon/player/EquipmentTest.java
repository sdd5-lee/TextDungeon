package com.textdungeon.player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.textdungeon.model.Item;

import org.junit.Before;
import org.junit.Test;

// ══════════════════════════════════════════════
//  Equipment 테스트
// ══════════════════════════════════════════════
public class EquipmentTest {

    private Equipment equipment;

    private Item item(String id, String type, int atk, int hp, int crit, int magic) {
        String json = String.format(
            "{\"id\":\"%s\",\"name\":\"%s\",\"type\":\"%s\"," +
            "\"hp\":%d,\"atk\":%d,\"value\":0,\"crit\":%d,\"magicDamage\":%d}",
            id, id, type, hp, atk, crit, magic);
        return Item.createFromJson(json);
    }

    @Before
    public void setUp() {
        equipment = new Equipment();
    }

    @Test
    public void equip_weapon_빈슬롯에_장착시_null반환() {
        Item old = equipment.equip(item("sword_01", "weapon", 10, 0, 0, 0), 0);
        assertNull(old);
    }

    @Test
    public void equip_weapon_기존무기_교체시_이전_무기_반환() {
        Item sword1 = item("sword_01", "weapon", 10, 0, 0, 0);
        Item sword2 = item("sword_02", "weapon", 20, 0, 0, 0);
        equipment.equip(sword1, 0);
        Item old = equipment.equip(sword2, 0);
        assertEquals(sword1, old);
        assertEquals(sword2, equipment.getWeapon());
    }

    @Test
    public void equip_armor_정상_장착() {
        Item armor = item("armor_01", "armor", 0, 50, 0, 0);
        equipment.equip(armor, 0);
        assertEquals(armor, equipment.getArmor());
    }

    @Test
    public void equip_artifact_슬롯0에_장착() {
        Item art = item("ring_01", "artifact", 0, 30, 5, 0);
        equipment.equip(art, 0);
        assertEquals(art, equipment.getArtifact()[0]);
    }

    @Test
    public void equip_artifact_슬롯1에_장착() {
        Item art = item("ring_02", "artifact", 0, 20, 3, 0);
        equipment.equip(art, 1);
        assertEquals(art, equipment.getArtifact()[1]);
    }

    @Test
    public void equip_artifact_유효하지않은_슬롯은_newItem반환() {
        Item art = item("ring_01", "artifact", 0, 30, 5, 0);
        Item returned = equipment.equip(art, 99);
        assertEquals(art, returned);
    }

    @Test
    public void equip_null_아이템은_null반환() {
        assertNull(equipment.equip(null, 0));
    }

    @Test
    public void getTotalAtk_모든_슬롯_합산() {
        equipment.equip(item("sword_01", "weapon", 10, 0, 0, 0), 0);
        equipment.equip(item("armor_01", "armor", 5, 0, 0, 0), 0);
        equipment.equip(item("ring_01", "artifact", 3, 0, 0, 0), 0);
        assertEquals(18, equipment.getTotalAtk());
    }

    @Test
    public void getTotalHp_모든_슬롯_합산() {
        equipment.equip(item("armor_01", "armor", 0, 50, 0, 0), 0);
        equipment.equip(item("ring_01", "artifact", 0, 30, 0, 0), 0);
        assertEquals(80, equipment.getTotalHp());
    }

    @Test
    public void getCrit_모든_슬롯_합산() {
        equipment.equip(item("sword_01", "weapon", 0, 0, 5, 0), 0);
        equipment.equip(item("ring_01", "artifact", 0, 0, 10, 0), 0);
        assertEquals(15, equipment.getCrit());
    }

    @Test
    public void getTotalMagicDamage_합산() {
        equipment.equip(item("staff_01", "weapon", 0, 0, 0, 20), 0);
        assertEquals(20, equipment.getTotalMagicDamage());
    }

    @Test
    public void unequip_weapon_정상_해제() {
        Item sword = item("sword_01", "weapon", 10, 0, 0, 0);
        equipment.equip(sword, 0);
        Item removed = equipment.unequip("weapon", 0);
        assertEquals(sword, removed);
        assertNull(equipment.getWeapon());
    }

    @Test
    public void unequip_빈슬롯은_null반환() {
        assertNull(equipment.unequip("weapon", 0));
    }

    @Test
    public void unequip_artifact_정상_해제() {
        Item art = item("ring_01", "artifact", 0, 0, 5, 0);
        equipment.equip(art, 0);
        Item removed = equipment.unequip("artifact", 0);
        assertEquals(art, removed);
        assertNull(equipment.getArtifact()[0]);
    }

    @Test
    public void 장비_없을때_모든_total은_0() {
        assertEquals(0, equipment.getTotalAtk());
        assertEquals(0, equipment.getTotalHp());
        assertEquals(0, equipment.getCrit());
        assertEquals(0, equipment.getTotalMagicDamage());
    }
}
