package com.textdungeon.model;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.textdungeon.data.DataControl;
import com.textdungeon.data.Difficulty;
import com.textdungeon.event.GameEvent;
import com.textdungeon.player.Player;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

// ══════════════════════════════════════════════
//  Reward.summary 테스트
//  혼돈의 주사위 보상설명 끝에 붙는 요약이 GameEvent.execute로 실제 적용되는 양과 같은지 확인
// ══════════════════════════════════════════════
public class RewardSummaryTest {

    private Player player;
    private DataControl<Item> itemManager;

    @Before
    public void setUp() {
        player = new Player("테스터", Job.WARRIOR);
        itemManager = mock(DataControl.class);
    }

    /** 주사위가 붙이는 것과 같은 방식으로 선택지 하나짜리 이벤트를 만든다 */
    private static GameEvent eventWith(Reward reward) {
        GameEvent event = new GameEvent();
        event.addChoiceWithReward("주사위 선택지", reward);
        return event;
    }

    @Test
    public void 모험의_축복과_어려움에서_경험치_요약이_실제_증가량과_같다() {
        player.setTraitId(Trait.BLESSING_OF_ADVENTURE.name());
        Reward reward = new Reward("묘사", null, Collections.singletonList(new RewardStat("경험치", 10)));

        String summary = reward.summary(Difficulty.HARD.rewardMultiplier, Reward.expMultiplier(player), null);
        int expBefore = player.getStat().getExp();
        eventWith(reward).execute(player, 0, itemManager, Difficulty.HARD);

        // 10 × 난이도 3 × 특성 2 = 60 (최대 경험치 100보다 작아서 레벨업으로 깎이지 않음)
        assertEquals("(경험치 +60)", summary);
        assertEquals(60, player.getStat().getExp() - expBefore);
    }

    @Test
    public void 보통_난이도_아이템은_두_개로_표시되고_실제로_두_개_받는다() {
        Item sword = Item.createFromJson("{\"id\":\"item_1\",\"name\":\"나무 검\",\"type\":\"weapon\"}");
        when(itemManager.spawn("item_1")).thenReturn(sword);
        Reward reward = new Reward("묘사", "item_1", Collections.emptyList());

        String summary = reward.summary(Difficulty.NORMAL.rewardMultiplier, Reward.expMultiplier(player), "나무 검");
        eventWith(reward).execute(player, 0, itemManager, Difficulty.NORMAL);

        assertEquals("(나무 검 ×2)", summary);
        assertEquals(2, player.getInventory().getCount("item_1"));
    }

    @Test
    public void 스탯_회복_피해는_부호와_배수를_붙인다() {
        Reward reward = new Reward("묘사", null, Arrays.asList(
                new RewardStat("힘", 3), new RewardStat("민첩", -1),
                new RewardStat("데미지", 5), new RewardStat("회복", 4)));

        assertEquals("(힘 +6, 민첩 -2, HP -10, HP +8)", reward.summary(2, 1, null));
    }

    @Test
    public void 특성이_없으면_경험치에_특성_배수를_곱하지_않는다() {
        assertEquals(1, Reward.expMultiplier(player));
        Reward reward = new Reward("묘사", null, Collections.singletonList(new RewardStat("경험치", 30)));
        assertEquals("(경험치 +30)", reward.summary(1, Reward.expMultiplier(player), null));
    }

    @Test
    public void 적용할_것이_없으면_빈_문자열() {
        assertEquals("", new Reward("묘사", null, Collections.emptyList()).summary(2, 1, null));
        // 아이템 이름을 모르면 요약에서 뺀다
        assertEquals("", new Reward("묘사", "item_x", Collections.emptyList()).summary(2, 1, null));
    }
}
