package com.textdungeon.model;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class StatTest {

    private Stat stat;

    @Before
    public void setUp() {
        // strength=10, agility=5, health=10, wisdom=4
        stat = new Stat(10, 5, 10, 4);
    }

    // ── 초기값 ──────────────────────────────────────────────

    @Test
    public void 초기_골드는_100() {
        assertEquals(100, stat.getGold());
    }

    @Test
    public void 초기_exp는_0() {
        assertEquals(0, stat.getExp());
    }

    @Test
    public void 초기_maxExp는_100() {
        assertEquals(100, stat.getMaxExp());
    }

    @Test
    public void 초기_statPoint는_0() {
        assertEquals(0, stat.getStatPoint());
    }

    @Test
    public void 생성자_직후_hp는_maxHp와_같음() {
        // updateBattleStat(0) 호출 후 hp = maxHp
        assertEquals(stat.getMaxHp(), stat.getHp());
    }

    // ── updateBattleStat ────────────────────────────────────

    @Test
    public void updateBattleStat_atk는_strength곱하기2더하기level() {
        stat.updateBattleStat(3);
        // atk = 10*2 + 3 = 23
        assertEquals(23, stat.getAtk());
    }

    @Test
    public void updateBattleStat_maxHp는_health곱하기10더하기level곱하기5() {
        stat.updateBattleStat(3);
        // maxHp = 10*10 + 3*5 = 115
        assertEquals(115, stat.getMaxHp());
    }

    @Test
    public void updateBattleStat_critical_rate는_agility더하기level() {
        stat.updateBattleStat(3);
        // critical_rate = 5 + 3 = 8
        assertEquals(8, stat.getCritical_rate());
    }

    @Test
    public void updateBattleStat_criticalRate는_100을_초과하지_않음() {
        Stat highAgi = new Stat(1, 200, 1, 1);
        highAgi.updateBattleStat(50);
        assertTrue(highAgi.getCritical_rate() <= 100);
    }

    @Test
    public void updateBattleStat_레벨업시_hp가_maxHp증가분만큼_올라감() {
        int hpBefore = stat.getHp();
        int maxHpBefore = stat.getMaxHp();
        stat.updateBattleStat(5);
        int maxHpAfter = stat.getMaxHp();
        int expectedHp = hpBefore + (maxHpAfter - maxHpBefore);
        assertEquals(expectedHp, stat.getHp());
    }

    @Test
    public void updateBattleStat_hp는_maxHp를_초과하지_않음() {
        stat.updateBattleStat(1);
        assertTrue(stat.getHp() <= stat.getMaxHp());
    }
    // ── gainStat ────────────────────────────────────────────

    @Test
    public void gainStat_회복_hp가_maxHp를_초과하지_않음() {
        stat.gainStat("회복", 99999);
        assertTrue(stat.getHp() <= stat.getMaxHp());
    }

    @Test
    public void gainStat_데미지_hp가_0_미만이_되지_않음() {
        stat.gainStat("데미지", 99999);
        assertTrue(stat.getHp() >= 0);
    }

    @Test
    public void gainStat_힘_증가() {
        int before = stat.getStrength();
        stat.gainStat("힘", 3);
        assertEquals(before + 3, stat.getStrength());
    }

    @Test
    public void gainStat_민첩_증가() {
        int before = stat.getAgility();
        stat.gainStat("민첩", 3);
        assertEquals(before + 3, stat.getAgility());
    }

    @Test
    public void gainStat_체력_증가() {
        int before = stat.getHealth();
        stat.gainStat("체력", 3);
        assertEquals(before + 3, stat.getHealth());
    }

    @Test
    public void gainStat_지혜_증가() {
        int before = stat.getWisdom();
        stat.gainStat("지혜", 3);
        assertEquals(before + 3, stat.getWisdom());
    }

    @Test
    public void gainStat_경험치_증가() {
        stat.gainStat("경험치", 50);
        assertEquals(50, stat.getExp());
    }

    @Test
    public void gainStat_골드_증가() {
        int before = stat.getGold();
        stat.gainStat("골드", 100);
        assertEquals(before + 100, stat.getGold());
    }

    // ── addGold 음수 방지 ────────────────────────────────────

    @Test
    public void addGold_음수가_되지_않음() {
        stat.addGold(-99999);
        assertEquals(0, stat.getGold());
    }

    // ── statPoint ───────────────────────────────────────────

    @Test
    public void addStatPoint_누적됨() {
        stat.addStatPoint(5);
        stat.addStatPoint(3);
        assertEquals(8, stat.getStatPoint());
    }
}
