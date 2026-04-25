package com.textdungeon.event;

import com.google.gson.annotations.SerializedName;
import com.textdungeon.data.DataControl;
import com.textdungeon.model.Item;
import com.textdungeon.model.Reward;
import com.textdungeon.player.Player;

import java.util.List;

public class GameEvent {
    @SerializedName("id") protected String id;
    @SerializedName("name") protected String name;
    protected String description;
    protected String imgId;
    protected int minFloor;
    protected int maxFloor;
    @SerializedName("rewards") protected List<Reward> rewards;

    @SerializedName("choices") protected List<String> choices;
    protected String enemyId;

    public GameEvent() {}

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }

    public int getMaxFloor() {
        return maxFloor;
    }

    public int getMinFloor() {
        return minFloor;
    }

    //추가
    public List<String> getChoices() {
        return choices;
    }

    public List<Reward> getRewards() {
        return rewards;
    }

    public String getEnemyId() {
        return enemyId;
    }

    public String getImgId() {
        return imgId;
    }

    public String execute(Player player, int choice, DataControl<Item> itemManager) {
        if (rewards == null || rewards.isEmpty() || choice >= rewards.size()) {
            return "보상은 없습니다";
        }
        Reward reward = rewards.get(choice);
        reward.apply(player, itemManager);
        player.getStat().updateBattleStat(player.getLevel());
        return reward.getDescription();
    }

}