package com.textdungeon.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.gson.Gson;
import com.textdungeon.ai.AiType;
import com.textdungeon.data.DataControl;
import com.textdungeon.data.EventGson;
import com.textdungeon.event.GameEvent;
import com.textdungeon.model.Item;
import com.textdungeon.model.Job;
import com.textdungeon.model.Monster;
import com.textdungeon.player.Player;

import org.junit.Before;
import org.junit.Test;

// ══════════════════════════════════════════════
//  시련의 신 결투 (GodTrial)
//  - 그 스탯 3당 적 +1%, 최대 2배
//  - 이기면 그 스탯만 (스탯의 1/4 + 2), 층별 상한까지, 던전 보상 배수만큼
// ══════════════════════════════════════════════
public class GodTrialTest {

    private Player player;
    private DataControl<Item> itemManager;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        player = new Player("테스터", Job.MAGE);
        itemManager = mock(DataControl.class);
    }

    /** 21층 지혜의 시련: 이기는 선택지(battle)와 피하는 선택지(escape) */
    private static GameEvent trialEvent(String stat) {
        String json = "{\"id\":\"event_magic_21\",\"name\":\"시험\",\"description\":\"설명\",\"type\":\"battle\","
                + "\"minFloor\":21,\"maxFloor\":21,\"enemyId\":\"mon_1\",\"trialStat\":\"" + stat + "\","
                + "\"choices\":[\"싸운다\",\"물러난다\"],"
                + "\"rewards\":[{\"action\":\"battle\",\"description\":\"이겼다.\",\"statRewards\":[]},"
                + "{\"action\":\"escape\",\"description\":\"물러났다.\",\"statRewards\":[]}]}";
        return EventGson.create().fromJson(json, GameEvent.class);
    }

    @Test
    public void 시련의_신은_넷이고_스탯이_하나씩이다() {
        assertEquals("지혜", AiType.MAGIC.getTrialStat());
        assertEquals("민첩", AiType.THIEF.getTrialStat());
        assertEquals("힘", AiType.STRENGTH.getTrialStat());
        assertEquals("체력", AiType.TRAINING.getTrialStat());
        assertFalse(AiType.STRUGGLE.isTrial());
        assertNull(AiType.CHAOS.getTrialStat());
        assertEquals("마법의 신", AiType.MAGIC.getGodName());
    }

    @Test
    public void 스탯이_높을수록_적이_세지고_두_배에서_멈춘다() {
        assertEquals(100, GodTrial.enemyPercent(0));
        assertEquals(100, GodTrial.enemyPercent(2));
        assertEquals(110, GodTrial.enemyPercent(30));
        assertEquals(150, GodTrial.enemyPercent(150));
        assertEquals(200, GodTrial.enemyPercent(300));
        assertEquals(200, GodTrial.enemyPercent(999));
    }

    @Test
    public void 보상은_스탯의_4분의_1_더하기_2이고_층별_상한까지() {
        assertEquals(2, GodTrial.reward("지혜", 0, 21));
        assertEquals(17, GodTrial.reward("지혜", 60, 21));
        // 21층 스탯 상한 26
        assertEquals(26, GodTrial.reward("지혜", 400, 21));
        // 1층 상한 6
        assertEquals(6, GodTrial.reward("힘", 40, 1));
    }

    @Test
    public void 스탯은_장비를_뺀_기본_스탯을_본다() {
        player.getStat().setStrength(31);
        assertEquals(31, GodTrial.statOf(player, "힘"));
        assertEquals(player.getStat().getWisdom(), GodTrial.statOf(player, "지혜"));
        assertEquals(player.getStat().getAgility(), GodTrial.statOf(player, "민첩"));
        assertEquals(player.getStat().getHealth(), GodTrial.statOf(player, "체력"));
        assertEquals(0, GodTrial.statOf(player, "골드"));
    }

    @Test
    public void 미리보기는_지금_스탯의_세기와_보상_배수를_곱한_양() {
        assertEquals("〔지혜의 시련〕 지금 지혜 60: 적이 20% 더 강하고, 이기면 지혜 +34",
                GodTrial.preview("지혜", 60, 21, 2));
        // 세지지 않으면 세기는 말하지 않는다
        assertEquals("〔힘의 시련〕 지금 힘 2: 이기면 힘 +2", GodTrial.preview("힘", 2, 21, 1));
    }

    @Test
    public void 이기면_그_스탯만_싸울_때의_스탯으로_오른다() {
        player.getStat().setWisdom(60);
        int gold = player.getStat().getGold();
        int exp = player.getStat().getExp();

        String result = trialEvent("지혜").execute(player, 0, itemManager, 2);

        assertEquals(60 + 17 * 2, player.getStat().getWisdom());
        assertEquals(gold, player.getStat().getGold());
        assertEquals(exp, player.getStat().getExp());
        assertEquals("이겼다. (지혜 +34)", result);
    }

    @Test
    public void 물러나면_아무것도_오르지_않고_이벤트의_보상은_그대로다() {
        player.getStat().setAgility(40);
        GameEvent event = trialEvent("민첩");

        assertEquals("물러났다.", event.execute(player, 1, itemManager, 2));
        assertEquals(40, player.getStat().getAgility());
        // 이긴 보상을 계산해도 저장된 보상 목록에는 스탯이 붙지 않는다 (이어하기 후 두 번 붙지 않게)
        event.execute(player, 0, itemManager, 1);
        assertTrue(event.getRewards().get(0).getStatRewards().isEmpty());
    }

    @Test
    public void 판_저장에서_다시_읽어도_시련이_남는다() {
        Gson gson = EventGson.create();
        GameEvent back = gson.fromJson(gson.toJson(trialEvent("체력")), GameEvent.class);
        assertEquals("체력", back.getTrialStat());
    }

    @Test
    public void 적은_체력과_공격력만_세지고_원본은_그대로() {
        Monster m = new Gson().fromJson("{\"id\":\"m\",\"name\":\"해골 기사\",\"hp\":200,\"maxHp\":200,\"attack\":30,"
                + "\"patterns\":[{\"name\":\"강타\",\"power\":150}]}", Monster.class);
        Monster strong = m.scaled(150);

        assertEquals(300, strong.getHp());
        assertEquals(300, strong.getMaxHp());
        assertEquals(45, strong.getAttack());
        assertEquals("해골 기사", strong.getName());
        assertEquals(1, strong.getPatterns().size());
        assertEquals(200, m.getHp());
        assertNotNull(strong.getId());
    }
}
