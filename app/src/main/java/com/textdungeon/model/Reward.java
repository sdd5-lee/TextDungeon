package com.textdungeon.model;

import com.textdungeon.data.DataControl;
import com.textdungeon.player.Player;

import java.util.List;

public class Reward {
    private String id;
    private String description;
    private String itemId;
    private List<RewardStat> statRewards;

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
                if (player.getTrait() != null && player.getTrait().modifyExp() > 1){
                    player.getStat().gainStatExp(statReward.getValue(),player.getTrait().modifyExp());
                }else {
                    player.getStat().gainStat(statReward.getStatType(), statReward.getValue());
                }
            }
            player.levelUp();
        }
    }
}
