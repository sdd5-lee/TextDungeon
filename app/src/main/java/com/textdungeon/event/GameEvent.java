package com.textdungeon.event;

import android.util.Log;
import android.widget.Toast;

import com.google.gson.annotations.SerializedName;
import com.textdungeon.data.DataControl;
import com.textdungeon.data.Difficulty;
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
    @SerializedName("type") protected String type;
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
    public String getItemId(int choice){
        Reward reward = getRewards().get(choice);
        return reward.getItemId();
    }

    public String execute(Player player, int choice, DataControl<Item> itemManager, Difficulty difficulty) {
        if (rewards == null || rewards.isEmpty() || choice >= rewards.size()) {
            return "보상은 없습니다";
        }
        Reward reward = rewards.get(choice);
        if (difficulty != null) {
            for (int i = 0; i < difficulty.rewardMultiplier; i++){
                reward.apply(player, itemManager);
                player.getStat().updateBattleStat(player.getLevel());
            }
        }else{
            reward.apply(player, itemManager);
            player.getStat().updateBattleStat(player.getLevel());
        }
        if (reward.getDescription() == null || reward.getDescription().isEmpty()) {
            return "신비로운 힘이 당신의 몸을 감싸다 지나갔습니다.";
        }
        return reward.getDescription();
    }
    public boolean isRetry(int choiceIndex) {
        if (rewards != null && choiceIndex < rewards.size()) {
            return rewards.get(choiceIndex).isRetry();
        }
        return false;
    }
    public boolean hasItemReward(int choiceIndex) {
        return getItemId(choiceIndex) != null;
    }
}