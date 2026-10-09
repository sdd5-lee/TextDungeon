package com.textdungeon.event;

import com.google.gson.annotations.SerializedName;
import com.textdungeon.data.DataControl;
import com.textdungeon.data.Difficulty;
import com.textdungeon.model.Item;
import com.textdungeon.model.Reward;
import com.textdungeon.player.Player;

import java.util.ArrayList;
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
            }
        }else{
            reward.apply(player, itemManager);
        }
        if (reward.getDescription() == null || reward.getDescription().isEmpty()) {
            return "신비로운 힘이 당신의 몸을 감싸다 지나갔습니다.";
        }
        return reward.getDescription();
    }
    /**
     * 선택지와 그에 대응하는 보상을 한 쌍으로 추가한다.
     * execute()가 rewards.get(choice)로 인덱스를 맞추기 때문에 둘은 반드시 같이 추가돼야 한다.
     * 메인 스레드에서만 호출할 것 (UI가 같은 리스트를 읽는다).
     */
    public void addChoiceWithReward(String choiceText, Reward reward) {
        if (choices == null) choices = new ArrayList<>();
        if (rewards == null) rewards = new ArrayList<>();
        if (choices.size() != rewards.size()) {
            throw new IllegalStateException("선택지(" + choices.size() + ")와 보상(" + rewards.size() + ") 개수가 달라 추가할 수 없습니다: " + id);
        }
        choices.add(choiceText);
        rewards.add(reward);
    }

    /** 선택지 추가가 가능한 상태인지 (선택지/보상 개수 정렬 여부) */
    public boolean canAppendChoice() {
        int c = choices == null ? 0 : choices.size();
        int r = rewards == null ? 0 : rewards.size();
        return c == r;
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