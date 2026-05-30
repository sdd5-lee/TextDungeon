package com.textdungeon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.textdungeon.model.Job;
import com.textdungeon.model.ShopUpgrade;
import com.textdungeon.system.UserRecord;

import org.junit.Before;
import org.junit.Test;

// ══════════════════════════════════════════════
//  UserRecord 테스트
// ══════════════════════════════════════════════
public class UserRecordTest {

    private UserRecord record;

    @Before
    public void setUp() {
        record = new UserRecord();
    }

    @Test
    public void 초기_gem은_0() {
        assertEquals(0, record.getGem());
    }

    @Test
    public void 초기_기본해금_직업들이_unlock되어있음() {
        assertTrue(record.isUnlockJob(Job.WARRIOR.name));
        assertTrue(record.isUnlockJob(Job.MAGE.name));
        assertTrue(record.isUnlockJob(Job.ROGUE.name));
        assertTrue(record.isUnlockJob(Job.ARCHER.name));
    }

    @Test
    public void 초기_잠금_직업들은_unlock_안됨() {
        assertFalse(record.isUnlockJob(Job.KNIGHT.name));
        assertFalse(record.isUnlockJob(Job.HERO.name));
    }

    @Test
    public void addGem_정상_증가() {
        record.addGem(500);
        assertEquals(500, record.getGem());
    }

    @Test
    public void deductGem_정상_감소() {
        record.addGem(500);
        record.deductGem(200);
        assertEquals(300, record.getGem());
    }

    @Test
    public void unlockJobs_직업_해금() {
        record.unlockJobs(Job.KNIGHT);
        assertTrue(record.isUnlockJob(Job.KNIGHT.name));
    }

    @Test
    public void getUpgradeLevel_초기값은_0() {
        assertEquals(0, record.getUpgradeLevel(ShopUpgrade.INIT_STAT_BONUS.name()));
    }

    @Test
    public void levelUpUpgrade_레벨_1_증가() {
        record.levelUpUpgrade(ShopUpgrade.INIT_STAT_BONUS.name());
        assertEquals(1, record.getUpgradeLevel(ShopUpgrade.INIT_STAT_BONUS.name()));
    }

    @Test
    public void levelUpUpgrade_여러번_누적() {
        record.levelUpUpgrade(ShopUpgrade.INIT_STAT_BONUS.name());
        record.levelUpUpgrade(ShopUpgrade.INIT_STAT_BONUS.name());
        record.levelUpUpgrade(ShopUpgrade.INIT_STAT_BONUS.name());
        assertEquals(3, record.getUpgradeLevel(ShopUpgrade.INIT_STAT_BONUS.name()));
    }

    @Test
    public void addKillCount_누적() {
        record.addKillCount();
        record.addKillCount();
        assertEquals(2, record.getKillCount());
    }

    @Test
    public void addClearCount_누적() {
        record.addClearCount();
        assertEquals(1, record.getClearCount());
    }
}
