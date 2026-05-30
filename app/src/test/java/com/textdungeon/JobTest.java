package com.textdungeon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.textdungeon.model.Job;

import org.junit.Test;

// ══════════════════════════════════════════════
//  Job enum 테스트
// ══════════════════════════════════════════════
public class JobTest {

    @Test
    public void 기본해금_직업들은_price가_0() {
        for (Job job : new Job[]{Job.WARRIOR, Job.MAGE, Job.ROGUE, Job.ARCHER}) {
            assertEquals(0, job.price);
            assertTrue(job.defaultUnlocked);
        }
    }

    @Test
    public void 잠금_직업들은_price가_0보다_큼() {
        for (Job job : new Job[]{Job.KNIGHT, Job.MONK, Job.CLERIC, Job.WARLOCK, Job.HERO}) {
            assertTrue(job.price > 0);
            assertFalse(job.defaultUnlocked);
        }
    }

    @Test
    public void 모든_직업_스탯이_양수() {
        for (Job job : Job.values()) {
            assertTrue(job.strength >= 0);
            assertTrue(job.agility >= 0);
            assertTrue(job.health >= 0);
            assertTrue(job.wisdom >= 0);
        }
    }

    @Test
    public void HERO가_가장_비쌈() {
        int heroPrice = Job.HERO.price;
        for (Job job : Job.values()) {
            assertTrue(job.price <= heroPrice);
        }
    }

    @Test
    public void valueOf_유효하지않은_이름은_예외() {
        try {
            Job.valueOf("INVALID_JOB");
            fail("예외가 발생해야 합니다");
        } catch (IllegalArgumentException e) {
            // 정상
        }
    }
}
