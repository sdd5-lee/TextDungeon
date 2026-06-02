package com.textdungeon.model;

import com.textdungeon.data.DataControl;
import com.textdungeon.data.Difficulty;
import com.textdungeon.event.GameEvent;
import com.textdungeon.player.Player;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


// ══════════════════════════════════════════════
//  GameEvent.execute 테스트
// ══════════════════════════════════════════════
public class GameEventExecuteTest {

    private Player player;
    private DataControl<Item> itemManager;

    @Before
    public void setUp() {
        player = new Player("테스터", com.textdungeon.model.Job.WARRIOR);
        itemManager = mock(DataControl.class);
    }

    private GameEvent makeEvent(String itemId, String statType, int statValue) {
        String rewardJson = String.format(
            "{\"itemId\":%s,\"statRewards\":[{\"type\":\"%s\",\"value\":%d}]}",
            itemId == null ? "null" : "\"" + itemId + "\"", statType, statValue);
        String json = String.format(
            "{\"id\":\"e1\",\"name\":\"테스트이벤트\",\"description\":\"테스트\"," +
            "\"type\":\"normal\",\"minFloor\":1,\"maxFloor\":50," +
            "\"choices\":[\"선택1\",\"선택2\"]," +
            "\"rewards\":[%s,%s]}", rewardJson, rewardJson);
        return new com.google.gson.Gson().fromJson(json, GameEvent.class);
    }

    @Test
    public void execute_rewards가_null이면_보상없음_반환() {
        String json = "{\"id\":\"e1\",\"name\":\"빈이벤트\",\"type\":\"normal\"," +
                "\"minFloor\":1,\"maxFloor\":50,\"choices\":[\"선택1\"]," +
                "\"rewards\":null}";
        GameEvent event = new com.google.gson.Gson().fromJson(json, GameEvent.class);

        String result = event.execute(player, 0, itemManager, Difficulty.NORMAL);
        assertEquals("보상은 없습니다", result);
    }

    @Test
    public void execute_choice가_범위초과면_보상없음_반환() {
        GameEvent event = makeEvent(null, "경험치", 10);
        String result = event.execute(player, 99, itemManager, Difficulty.NORMAL);
        assertEquals("보상은 없습니다", result);
    }

    @Test
    public void execute_EASY_rewardMultiplier1_스탯_1회만_적용() {
        GameEvent event = makeEvent(null, "경험치", 50);
        event.execute(player, 0, itemManager, Difficulty.EASY);
        // EASY rewardMultiplier=1 → 경험치 50 × 1 = 50
        assertEquals(50, player.getStat().getExp());
    }

    @Test
    public void execute_NORMAL_rewardMultiplier2_스탯_2회_적용() {
        GameEvent event = makeEvent(null, "골드", 100);
        int goldBefore = player.getStat().getGold();
        event.execute(player, 0, itemManager, Difficulty.NORMAL);
        // NORMAL rewardMultiplier=2 → 골드 100 × 2 = 200
        assertEquals(goldBefore + 200, player.getStat().getGold());
    }

    @Test
    public void execute_HARD_rewardMultiplier3_스탯_3회_적용() {
        GameEvent event = makeEvent(null, "골드", 100);
        int goldBefore = player.getStat().getGold();
        event.execute(player, 0, itemManager, Difficulty.HARD);
        // HARD rewardMultiplier=3 → 골드 100 × 3 = 300
        assertEquals(goldBefore + 300, player.getStat().getGold());
    }

    @Test
    public void execute_difficulty_null이면_1회만_적용() {
        GameEvent event = makeEvent(null, "경험치", 50);
        event.execute(player, 0, itemManager, null);
        assertEquals(50, player.getStat().getExp());
    }

    @Test
    public void execute_아이템보상_NORMAL난이도_아이템_2번_지급() {
        // rewardMultiplier=2 → pickUpItem 2회 호출
        Item potion = Item.createFromJson(
            "{\"id\":\"potion_01\",\"name\":\"포션\",\"type\":\"consumables\"," +
            "\"hp\":20,\"atk\":0,\"value\":0,\"crit\":0,\"magicDamage\":0}");
        when(itemManager.spawn("potion_01")).thenReturn(potion);

        GameEvent event = makeEvent("potion_01", "경험치", 0);
        event.execute(player, 0, itemManager, Difficulty.NORMAL);

        // 같은 아이템 2번 지급 → 수량 2
        assertEquals(2, (int) player.getInventory().getItemMap().get("potion_01"));
    }

    @Test
    public void getItemId_정상_반환() {
        GameEvent event = makeEvent("potion_01", "경험치", 10);
        assertEquals("potion_01", event.getItemId(0));
    }
}
