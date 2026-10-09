package com.textdungeon.model;

import com.textdungeon.data.DataControl;
import com.textdungeon.player.Player;

import java.util.ArrayList;
import java.util.List;

public class Reward {
    private String id;
    private String description;
    private String itemId;
    private List<RewardStat> statRewards;
    private boolean retry;
    private String action;

    /** Gson 역직렬화용 기본 생성자 */
    public Reward() {}

    /** AI가 만든 보상을 코드에서 직접 생성할 때 사용 */
    public Reward(String description, String itemId, List<RewardStat> statRewards) {
        this.description = description;
        this.itemId = itemId;
        this.statRewards = statRewards != null ? new ArrayList<>(statRewards) : new ArrayList<>();
        this.retry = false;
        this.action = null;
    }

    public String getAction() {
        return action;
    }
    public boolean isRetry() {
        return retry;
    }
    public String getId() {
        return id;
    }
    public String getDescription() {
        return description;
    }
    public String getItemId() {
        return itemId;
    }
    public void apply(Player player, DataControl<Item> itemManager) {
        if (itemId != null) {
            player.pickUpItem(itemManager.spawn(itemId));
        }
        if (statRewards != null) {
            int wisdomBefore = player.getStat().getWisdom();
            for (RewardStat statReward : statRewards) {
                String type = statReward.getStatType();
                int value = statReward.getValue();
                if ("회복".equals(type)) {
                    player.heal(value);
                } else if ("데미지".equals(type)) {
                    player.takeDamage(value);
                } else {
                    if ("경험치".equals(type) && player.getTrait() != null && player.getTrait().modifyExp() > 1) {
                        player.getStat().gainStatExp(value, player.getTrait().modifyExp());
                    } else {
                        player.getStat().gainStat(type, value);
                    }
                }
            }
            // 스탯 보상이 공격력/최대HP/치명타/마법 횟수에 바로 반영되도록 재계산 (예전엔 레벨업할 때만 반영됨)
            player.recalculateStats(wisdomBefore);
            player.levelUp();
        }
    }
}