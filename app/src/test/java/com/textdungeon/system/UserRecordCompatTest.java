package com.textdungeon.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

// ══════════════════════════════════════════════
//  결제 필드가 없던 예전 user_record.json 호환
// ══════════════════════════════════════════════
public class UserRecordCompatTest {

    @Test
    public void 예전_기록은_보유_상품이_비어_있고_기존_값은_그대로() {
        String old = "{\"gem\":1234,\"unlockJobName\":[\"전사\",\"기사\"],\"unlockTraitName\":[\"INDOMITABLE\"],"
                + "\"upgradeLevels\":{\"INIT_GOLD_BONUS\":3},\"killCount\":5,\"clearCount\":1}";

        UserRecord r = new Gson().fromJson(old, UserRecord.class);

        assertNotNull(r.getOwnedProducts());
        assertTrue(r.getOwnedProducts().isEmpty());
        assertNull(r.getLastScenarioId());
        assertEquals(1234, r.getGem());
        assertTrue(r.isUnlockJob("기사"));
        assertEquals(3, r.getUpgradeLevel("INIT_GOLD_BONUS"));
    }

    @Test
    public void 저장_후_다시_읽어도_보유_상품과_시나리오가_남는다() {
        UserRecord r = new UserRecord();
        r.setOwnedProducts(new HashSet<>(Arrays.asList("remove_ads", "gem_boost_1")));
        r.setLastScenarioId("base");

        UserRecord back = new Gson().fromJson(new Gson().toJson(r), UserRecord.class);

        assertEquals(new HashSet<>(Arrays.asList("remove_ads", "gem_boost_1")), back.getOwnedProducts());
        assertEquals("base", back.getLastScenarioId());
        assertTrue(back.getEntitlementSyncedAt() > 0);
    }
}
