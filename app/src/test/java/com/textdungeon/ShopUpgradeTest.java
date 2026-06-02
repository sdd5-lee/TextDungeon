package com.textdungeon;

import com.textdungeon.model.ShopUpgrade;
import org.junit.Test;
import static org.junit.Assert.*;


// ══════════════════════════════════════════════
//  ShopUpgrade enum 테스트
// ══════════════════════════════════════════════
public class ShopUpgradeTest {

    @Test
    public void getNextPrice_레벨0일때_기본가격() {
        ShopUpgrade upgrade = ShopUpgrade.INIT_STAT_BONUS;
        // basePrice=500, priceIncreasePerLevel=500, level=0
        assertEquals(500, upgrade.getNextPrice(0));
    }

    @Test
    public void getNextPrice_레벨1일때_가격_증가() {
        ShopUpgrade upgrade = ShopUpgrade.INIT_STAT_BONUS;
        // 500 + (1 * 500) = 1000
        assertEquals(1000, upgrade.getNextPrice(1));
    }

    @Test
    public void getNextPrice_레벨이_높아질수록_가격_증가() {
        ShopUpgrade upgrade = ShopUpgrade.INIT_STAT_STRENGTH;
        int price0 = upgrade.getNextPrice(0);
        int price1 = upgrade.getNextPrice(1);
        int price2 = upgrade.getNextPrice(2);
        assertTrue(price0 < price1);
        assertTrue(price1 < price2);
    }

    @Test
    public void 모든_업그레이드_maxLevel이_양수() {
        for (ShopUpgrade u : ShopUpgrade.values()) {
            assertTrue(u.title + "의 maxLevel이 0 이하", u.maxLevel > 0);
        }
    }

    @Test
    public void 모든_업그레이드_valuePerLevel이_양수() {
        for (ShopUpgrade u : ShopUpgrade.values()) {
            assertTrue(u.title + "의 valuePerLevel이 0 이하", u.valuePerLevel > 0);
        }
    }
}


