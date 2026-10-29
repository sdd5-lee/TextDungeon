package com.textdungeon.billing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

// ══════════════════════════════════════════════
//  Entitlements 테스트: 보유 상품 → 광고 스킵, 부스터 배율, 구매 가능 여부
// ══════════════════════════════════════════════
public class EntitlementsTest {

    private static Entitlements owning(Product... products) {
        Set<String> ids = new HashSet<>();
        for (Product p : products) ids.add(p.id);
        return new Entitlements(ids);
    }

    @Test
    public void 보유_없음이면_배율_1_광고_스킵_없음() {
        Entitlements e = new Entitlements(null);
        assertEquals(1, e.gemBoostMultiplier());
        assertFalse(e.hasAdSkip());
    }

    @Test
    public void 부스터_개수만큼_배율이_오른다() {
        assertEquals(2, owning(Product.GEM_BOOST_1).gemBoostMultiplier());
        assertEquals(3, owning(Product.GEM_BOOST_1, Product.GEM_BOOST_2).gemBoostMultiplier());
        assertEquals(4, owning(Product.GEM_BOOST_1, Product.GEM_BOOST_2, Product.GEM_BOOST_3).gemBoostMultiplier());
    }

    @Test
    public void 부스터_II만_있어도_배율은_개수로_계산() {
        assertEquals(2, owning(Product.GEM_BOOST_2).gemBoostMultiplier());
    }

    @Test
    public void 광고_스킵_보유() {
        assertTrue(owning(Product.REMOVE_ADS).hasAdSkip());
        assertEquals(1, owning(Product.REMOVE_ADS).gemBoostMultiplier());
    }

    @Test
    public void 부스터는_앞_단계가_있어야_살_수_있다() {
        assertFalse(owning().canBuy(Product.GEM_BOOST_2));
        assertTrue(owning(Product.GEM_BOOST_1).canBuy(Product.GEM_BOOST_2));
        assertTrue(owning().canBuy(Product.GEM_BOOST_1));
        assertTrue(owning().canBuy(Product.REMOVE_ADS));
    }

    @Test
    public void 이미_보유한_상품은_살_수_없다() {
        assertFalse(owning(Product.REMOVE_ADS).canBuy(Product.REMOVE_ADS));
        assertFalse(owning(Product.GEM_BOOST_1).canBuy(Product.GEM_BOOST_1));
    }

    @Test
    public void 모르는_ID는_권한에_영향이_없다() {
        Entitlements e = new Entitlements(new HashSet<>(Arrays.asList("old_product", "gem_boost_1")));
        assertEquals(2, e.gemBoostMultiplier());
        assertFalse(e.hasAdSkip());
    }
}
