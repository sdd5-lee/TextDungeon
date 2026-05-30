package com.textdungeon.player;

import com.textdungeon.model.Item;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

// ══════════════════════════════════════════════
//  Inventory 테스트
// ══════════════════════════════════════════════
public class InventoryTest {

    private Inventory inventory;

    private Item item(String id, String type) {
        String json = String.format(
            "{\"id\":\"%s\",\"name\":\"아이템_%s\",\"type\":\"%s\"," +
            "\"hp\":10,\"atk\":5,\"value\":10,\"crit\":0,\"magicDamage\":0}", id, id, type);
        return Item.createFromJson(json);
    }

    @Before
    public void setUp() {
        inventory = new Inventory();
    }

    @Test
    public void addItem_새_아이템_추가() {
        inventory.addItem(item("sword_01", "weapon"));
        assertTrue(inventory.isItem("sword_01"));
        assertEquals(1, (int) inventory.getItemMap().get("sword_01"));
    }

    @Test
    public void addItem_같은_아이템_스택_증가() {
        Item sword = item("sword_01", "weapon");
        inventory.addItem(sword);
        inventory.addItem(sword);
        // 종류 수는 1, 수량은 2
        assertEquals(1, inventory.getItemMap().size());
        assertEquals(2, (int) inventory.getItemMap().get("sword_01"));
    }

    @Test
    public void addItem_null_무시() {
        inventory.addItem(null);
        assertEquals(0, inventory.getItemMap().size());
    }

    @Test
    public void addItem_30종_초과시_새_종류_추가_안됨() {
        for (int i = 0; i < 30; i++) {
            inventory.addItem(item("item_" + i, "consumables"));
        }
        inventory.addItem(item("overflow", "consumables"));
        assertFalse(inventory.isItem("overflow"));
        assertEquals(30, inventory.getItemMap().size());
    }

    @Test
    public void addItem_기존_아이템_스택은_30종_초과해도_가능() {
        // 30종 채운 뒤 기존 아이템 수량 증가는 허용됨
        for (int i = 0; i < 30; i++) {
            inventory.addItem(item("item_" + i, "consumables"));
        }
        inventory.addItem(item("item_0", "consumables")); // 기존 아이템 재추가
        assertEquals(2, (int) inventory.getItemMap().get("item_0"));
    }

    @Test
    public void consumeItem_수량2에서_1로_감소() {
        Item sword = item("sword_01", "weapon");
        inventory.addItem(sword);
        inventory.addItem(sword);
        assertTrue(inventory.consumeItem("sword_01"));
        assertEquals(1, (int) inventory.getItemMap().get("sword_01"));
    }

    @Test
    public void consumeItem_수량1이면_맵에서_제거() {
        inventory.addItem(item("sword_01", "weapon"));
        inventory.consumeItem("sword_01");
        assertFalse(inventory.isItem("sword_01"));
    }

    @Test
    public void consumeItem_없는_아이템은_false() {
        assertFalse(inventory.consumeItem("nonexistent"));
    }

    @Test
    public void removeItem_완전_제거() {
        Item sword = item("sword_01", "weapon");
        inventory.addItem(sword);
        inventory.addItem(sword); // 수량 2
        inventory.removeItem("sword_01");
        assertFalse(inventory.isItem("sword_01"));
    }

    @Test
    public void isFullItem_29종이면_false() {
        for (int i = 0; i < 29; i++) {
            inventory.addItem(item("item_" + i, "consumables"));
        }
        assertFalse(inventory.isFullItem());
    }

    @Test
    public void isFullItem_30종이면_true() {
        for (int i = 0; i < 30; i++) {
            inventory.addItem(item("item_" + i, "consumables"));
        }
        assertTrue(inventory.isFullItem());
    }
}


