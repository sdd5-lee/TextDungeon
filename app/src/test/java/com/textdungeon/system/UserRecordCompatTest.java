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

    @Test
    public void 예전_기록은_클리어한_던전이_없고_클리어하면_저장된다() {
        String old = "{\"gem\":0,\"unlockJobName\":[\"전사\"],\"clearCount\":2}";
        UserRecord r = new Gson().fromJson(old, UserRecord.class);
        assertTrue(r.getClearedDungeons().isEmpty());

        r.addClearedDungeon("dungeon_1");
        r.addClearedDungeon(null);
        UserRecord back = new Gson().fromJson(new Gson().toJson(r), UserRecord.class);
        assertEquals(new HashSet<>(Arrays.asList("dungeon_1")), back.getClearedDungeons());

        back.clearAll();
        assertTrue(back.getClearedDungeons().isEmpty());
    }

    @Test
    public void 최근_이름은_최신이_앞이고_3개까지_겹치지_않게() {
        String old = "{\"gem\":0,\"unlockJobName\":[\"전사\"]}";
        UserRecord r = new Gson().fromJson(old, UserRecord.class);
        assertTrue(r.getRecentNames().isEmpty());

        r.addRecentName("브란");
        r.addRecentName("카엘");
        r.addRecentName("세라");
        r.addRecentName("브란");
        r.addRecentName("로웬");

        UserRecord back = new Gson().fromJson(new Gson().toJson(r), UserRecord.class);
        assertEquals(Arrays.asList("로웬", "브란", "세라"), back.getRecentNames());
    }
}
