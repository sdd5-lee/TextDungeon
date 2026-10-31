package com.textdungeon.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.textdungeon.model.Job;
import com.textdungeon.model.Monster;
import com.textdungeon.player.Player;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Random;

// ══════════════════════════════════════════════
//  BattleSystem의 적 턴: 기술 피해, 연타, 기운 모으기, 보스 분노
//  적 턴의 난수는 EnemyAi.choose 한 번 → 피해 흔들림(nextInt(3)) 한 번 순서로 쓴다
// ══════════════════════════════════════════════
public class BattleSystemPatternTest {

    private Player player;

    /** 정해 둔 값을 차례로 돌려준다 */
    private static class FixedRandom extends Random {
        final Deque<Integer> values = new ArrayDeque<>();

        FixedRandom(Integer... values) {
            this.values.addAll(Arrays.asList(values));
        }

        @Override
        public int nextInt(int bound) {
            Integer v = values.poll();
            return v != null ? v : 0;
        }
    }

    @Before
    public void setUp() {
        player = new Player("테스터", Job.WARRIOR);
        player.setTraitId(null); // 특성이 피해를 바꾸지 않게
        player.getStat().setHp(10_000);
    }

    private static Monster monster(int hp, int attack, String patternsJson) {
        return new Gson().fromJson("{\"id\":\"m\",\"name\":\"시험 몬스터\",\"hp\":" + hp + ",\"maxHp\":" + hp
                + ",\"attack\":" + attack + ",\"patterns\":" + patternsJson + "}", Monster.class);
    }

    private static BattleSystem battle(Player player, Monster monster, int statMultiplier, Integer... randoms) throws Exception {
        BattleSystem b = new BattleSystem(player, monster, null, statMultiplier);
        Field f = BattleSystem.class.getDeclaredField("random");
        f.setAccessible(true);
        f.set(b, new FixedRandom(randoms));
        return b;
    }

    /** 방어로 내 턴을 넘기고(공격 난수를 쓰지 않는다) 적 턴의 기록을 돌려준다 */
    private String defendThenEnemy(BattleSystem b) {
        b.playerAction(2, null);
        return b.enemyAction();
    }

    @Test
    public void 기술이_없으면_평타() throws Exception {
        BattleSystem b = battle(player, monster(100, 20, "[]"), 1, 2);
        int before = player.getStat().getHp();
        String log = defendThenEnemy(b);
        assertEquals((20 + 2) / 2, before - player.getStat().getHp());
        assertTrue(log, log.contains("시험 몬스터의 공격!"));
    }

    @Test
    public void 기술은_공격력_퍼센트_곱하기_타수() throws Exception {
        // choose: nextInt(3)=0 → 기술, 흔들림 0 → 20 * 70% * 2연타 = 28, 방어로 절반 = 14
        BattleSystem b = battle(player, monster(100, 20,
                "[{\"name\":\"난타\",\"power\":70,\"hits\":2,\"charge\":false}]"), 1, 0, 0);
        int before = player.getStat().getHp();
        String log = defendThenEnemy(b);
        assertEquals(14, before - player.getStat().getHp());
        assertTrue(log, log.contains("「난타」") && log.contains("2연타"));
    }

    @Test
    public void 던전_배수는_기술_피해에도_곱해진다() throws Exception {
        BattleSystem b = battle(player, monster(100, 20,
                "[{\"name\":\"강타\",\"power\":150,\"hits\":1,\"charge\":false}]"), 2, 0, 0);
        int before = player.getStat().getHp();
        defendThenEnemy(b);
        assertEquals(40 * 150 / 100 / 2, before - player.getStat().getHp());
        assertEquals(200, b.getEnemyHp());
    }

    @Test
    public void 기운을_모으는_턴엔_공격하지_않고_다음_턴에_쓴다() throws Exception {
        BattleSystem b = battle(player, monster(100, 20,
                "[{\"name\":\"브레스\",\"power\":250,\"hits\":1,\"charge\":true}]"), 1, 0, 0);
        int before = player.getStat().getHp();
        String first = defendThenEnemy(b);
        assertEquals(before, player.getStat().getHp());
        assertTrue(first, first.contains("기운을 모읍니다"));
        assertFalse(b.isEnemyTurn()); // 내 차례로 돌아온다

        // 다음 적 턴: choose를 부르지 않고 모은 기술을 쓴다. 흔들림 0 → 20 * 250% = 50, 방어로 25
        String second = defendThenEnemy(b);
        assertEquals(25, before - player.getStat().getHp());
        assertTrue(second, second.contains("「브레스」"));
    }

    @Test
    public void 보스는_체력이_절반_이하가_되면_한_번만_분노를_알린다() throws Exception {
        Monster boss = monster(100, 10, "[{\"name\":\"일격\",\"power\":150,\"hits\":1,\"charge\":false},"
                + "{\"name\":\"필살기\",\"power\":300,\"hits\":1,\"charge\":false}]");
        // 1턴: 체력 가득 → nextInt(2)=1 평타, 흔들림 0
        // 2턴(체력 절반으로 깎은 뒤): nextInt(3)=2 → 두 번째 기술, 흔들림 0
        // 3턴: nextInt(3)=0 평타
        BattleSystem b = battle(player, boss, 1, 1, 0, 2, 0, 0, 0);
        String calm = defendThenEnemy(b);
        assertFalse(calm, calm.contains("분노"));

        Field hp = BattleSystem.class.getDeclaredField("enemyHp");
        hp.setAccessible(true);
        hp.setInt(b, 50);

        String angry = defendThenEnemy(b);
        assertTrue(angry, angry.contains("분노합니다") && angry.contains("「필살기」"));
        String again = defendThenEnemy(b);
        assertFalse(again, again.contains("분노"));
    }
}
