package com.textdungeon.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.textdungeon.data.DataControl;
import com.textdungeon.player.Player;

import org.junit.Before;
import org.junit.Test;

// ══════════════════════════════════════════════
//  Reward.apply 테스트
// ══════════════════════════════════════════════
public class RewardTest {

    private Player player;
    private DataControl<Item> itemManager;

    private Item item(String id, String type, int hp) {
        String json = String.format(
            "{\"id\":\"%s\",\"name\":\"%s\",\"type\":\"%s\"," +
            "\"hp\":%d,\"atk\":0,\"value\":0,\"crit\":0,\"magicDamage\":0}",
            id, id, type, hp);
        return Item.createFromJson(json);
    }

    @Before
    public void setUp() {
        player = new Player("테스터", Job.WARRIOR);
        itemManager = mock(DataControl.class);
    }

    @Test
    public void apply_itemId가_null이면_아이템_지급_없음() {
        // itemId=null, statRewards=null 인 Reward
        String json = "{\"id\":\"r1\",\"description\":\"보상\",\"itemId\":null,\"statRewards\":null}";
        Reward reward = new com.google.gson.Gson().fromJson(json, Reward.class);

        reward.apply(player, itemManager);

        // itemManager.spawn 호출 안 됨
        verify(itemManager, never()).spawn(any());
    }

    @Test
    public void apply_itemId가_있으면_아이템_지급() {
        Item potion = item("potion_01", "consumables", 20);
        when(itemManager.spawn("potion_01")).thenReturn(potion);

        String json = "{\"id\":\"r1\",\"description\":\"포션 지급\",\"itemId\":\"potion_01\",\"statRewards\":null}";
        Reward reward = new com.google.gson.Gson().fromJson(json, Reward.class);

        reward.apply(player, itemManager);

        assertTrue(player.getInventory().isItem("potion_01"));
    }

    @Test
    public void apply_statRewards가_있으면_스탯_적용() {
        String json = "{\"id\":\"r1\",\"description\":\"경험치\",\"itemId\":null," +
                "\"statRewards\":[{\"type\":\"경험치\",\"value\":50}]}";
        Reward reward = new com.google.gson.Gson().fromJson(json, Reward.class);

        reward.apply(player, itemManager);

        // 경험치 50 적용 후 levelUp() 호출됨
        // exp < maxExp(100)이므로 레벨업 안 됨, 경험치만 50 증가
        assertEquals(50, player.getStat().getExp());
    }

    @Test
    public void apply_statRewards_경험치_충분하면_레벨업() {
        String json = "{\"id\":\"r1\",\"description\":\"대량경험치\",\"itemId\":null," +
                "\"statRewards\":[{\"type\":\"경험치\",\"value\":100}]}";
        Reward reward = new com.google.gson.Gson().fromJson(json, Reward.class);

        reward.apply(player, itemManager);

        assertEquals(2, player.getLevel());
    }
}
