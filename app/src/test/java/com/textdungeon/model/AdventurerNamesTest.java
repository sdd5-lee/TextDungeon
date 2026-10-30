package com.textdungeon.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Random;

// ══════════════════════════════════════════════
//  이름 정하기 창의 주사위 이름
// ══════════════════════════════════════════════
public class AdventurerNamesTest {

    @Test
    public void 모든_직업의_주사위_이름이_10글자를_넘지_않는다() {
        Random rnd = new Random(1);
        for (Job job : Job.values()) {
            for (int i = 0; i < 200; i++) {
                String name = AdventurerNames.random(job, rnd);
                assertFalse(job + " 이름이 비었다", name.trim().isEmpty());
                assertTrue(job + " " + name, name.length() <= AdventurerNames.MAX_LENGTH);
            }
        }
    }

    @Test
    public void 주사위_이름은_그_직업의_수식어로_시작한다() {
        Random rnd = new Random(2);
        for (Job job : Job.values()) {
            String[] epithets = AdventurerNames.epithetsOf(job);
            assertNotNull(job + " 수식어가 없다", epithets);
            String name = AdventurerNames.random(job, rnd);
            boolean matched = false;
            for (String e : epithets) {
                if (name.startsWith(e + " ")) matched = true;
            }
            assertTrue(job + " " + name, matched);
        }
    }
}
