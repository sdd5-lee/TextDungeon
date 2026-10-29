package com.textdungeon.billing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

// ══════════════════════════════════════════════
//  Product 테스트: Play Console 상품 ID 형식과 단계
// ══════════════════════════════════════════════
public class ProductTest {

    @Test
    public void fromId로_모든_상품이_되돌아온다() {
        for (Product p : Product.values()) {
            assertEquals(p, Product.fromId(p.id));
        }
        assertNull(Product.fromId("no_such_product"));
        assertNull(Product.fromId(null));
    }

    @Test
    public void 상품_ID는_Play_형식이고_겹치지_않는다() {
        Set<String> ids = new HashSet<>();
        for (Product p : Product.values()) {
            assertTrue(p.id, p.id.matches("[a-z][a-z0-9_.]*"));
            assertTrue("중복 ID: " + p.id, ids.add(p.id));
        }
    }

    @Test
    public void 부스터_앞_단계() {
        assertNull(Product.GEM_BOOST_1.previousTier());
        assertEquals(Product.GEM_BOOST_1, Product.GEM_BOOST_2.previousTier());
        assertEquals(Product.GEM_BOOST_2, Product.GEM_BOOST_3.previousTier());
        assertNull(Product.REMOVE_ADS.previousTier());
    }
}
