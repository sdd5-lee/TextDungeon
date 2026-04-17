package com.textdungeon.event;

import com.textdungeon.data.DataControl;
import com.textdungeon.model.Item;
import com.textdungeon.model.Reward;
import com.textdungeon.player.Player;

import java.util.List;

public class GameEvent {
    protected String id;
    protected String name;
    protected String description;
    protected String imgId;
    protected int minFloor;
    protected int maxFloor;
    protected List<Reward> rewards;

    //json에 있는 choices랑 enemyId
    protected List<String> choices;
    protected String enemyId;

    public GameEvent() {}

    // 기본 Getter들
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
        return reward.getDescription();
    }

}