package com.textdungeon.model;

import com.textdungeon.data.DataControl;
import com.textdungeon.player.Player;

import java.util.List;

public class Reward {
    private String id;
    private String description;
    private String itemId;
    private List<RewardStat> statRewards;
    private boolean retry;

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
            for (RewardStat statReward : statRewards) {
                String type = statReward.getStatType();
                int value = statReward.getValue();
                if ("회복".equals(type)) {
                    player.heal(value);
                } else if ("데미지".equals(type)) {
                    player.takeDamage(value);
                } else {
                    if (player.getTrait() != null && player.getTrait().modifyExp() > 1) {
                        player.getStat().gainStatExp(value, player.getTrait().modifyExp());
                    } else {
                        player.getStat().gainStat(type, value);
                    }
                }
            }
            player.levelUp();
        }
    }
}
