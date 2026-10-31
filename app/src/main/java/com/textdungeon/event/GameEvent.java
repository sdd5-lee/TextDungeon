package com.textdungeon.event;

import com.google.gson.annotations.SerializedName;
import com.textdungeon.data.DataControl;
import com.textdungeon.model.Item;
import com.textdungeon.model.Reward;
import com.textdungeon.model.RewardStat;
import com.textdungeon.player.Player;
import com.textdungeon.system.GodTrial;

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
    /** 이 던전들에서만 나온다 (dungeon_list.json의 id). 없으면 모든 던전에서 나온다 */
    protected List<String> dungeons;
    /**
     * 시련의 신 결투 (힘, 민첩, 체력, 지혜). 이 스탯이 높을수록 적이 세지고, 이기면 이 스탯이 오른다 (GodTrial).
     * 없으면 보통 이벤트
     */
    protected String trialStat;

    // 혼돈의 주사위 상태. 판 저장 파일(GameSave)에 이벤트와 같이 저장돼서 이어하기에도 남는다
    /** 다시 굴릴 수 있는 결과표 선택지의 자리. 없으면 null */
    protected Integer chaosSlot;
    /** 이 이벤트에서는 주사위를 더 쓸 수 없음 (AI·특별 선택지가 나왔거나 다시 굴리기를 씀) */
    protected boolean chaosDone;

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

    public String getTrialStat() {
        return trialStat;
    }

    /** 이 던전에서 나올 수 있는지 */
    public boolean appearsIn(String dungeonId) {
        return dungeons == null || dungeons.isEmpty() || dungeons.contains(dungeonId);
    }
    public String getItemId(int choice){
        Reward reward = getRewards().get(choice);
        return reward.getItemId();
    }

    /** @param rewardMultiplier 던전의 보상 배수. 보상 전체를 이만큼 반복 적용한다 (1 미만이면 한 번) */
    public String execute(Player player, int choice, DataControl<Item> itemManager, int rewardMultiplier) {
        if (rewards == null || rewards.isEmpty() || choice >= rewards.size()) {
            return "보상은 없습니다";
        }
        Reward reward = rewards.get(choice);
        // 시련에서 이기면 그 스탯이 오른다. 양은 싸운 그때의 스탯으로 정하므로 올리기 전에 잰다
        String trialGain = null;
        if (trialStat != null && "battle".equals(reward.getAction())) {
            int amount = GodTrial.reward(trialStat, GodTrial.statOf(player, trialStat), minFloor);
            reward = withTrialGain(reward, amount);
            trialGain = "(" + trialStat + " +" + amount * Math.max(1, rewardMultiplier) + ")";
        }
        for (int i = 0; i < Math.max(1, rewardMultiplier); i++) {
            reward.apply(player, itemManager);
        }
        String description = reward.getDescription();
        if (description == null || description.isEmpty()) {
            description = "신비로운 힘이 당신의 몸을 감싸다 지나갔습니다.";
        }
        return trialGain != null ? description + " " + trialGain : description;
    }

    /** 원래 보상(아이템·스탯)에 시련의 스탯을 더한 새 보상. 이벤트에 저장된 보상은 바꾸지 않는다 */
    private Reward withTrialGain(Reward reward, int amount) {
        List<RewardStat> stats = new ArrayList<>();
        if (reward.getStatRewards() != null) stats.addAll(reward.getStatRewards());
        stats.add(new RewardStat(trialStat, amount));
        return new Reward(reward.getDescription(), reward.getItemId(), stats);
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

    /** index 자리의 선택지와 보상을 같이 바꾼다 (주사위 다시 굴리기). 메인 스레드에서만 호출할 것 */
    public void replaceChoiceWithReward(int index, String choiceText, Reward reward) {
        if (!canAppendChoice() || choices == null || index < 0 || index >= choices.size()) {
            throw new IllegalStateException("선택지 " + index + "번을 바꿀 수 없습니다: " + id);
        }
        choices.set(index, choiceText);
        rewards.set(index, reward);
    }

    public Integer getChaosSlot() { return chaosSlot; }
    public boolean isChaosDone() { return chaosDone; }

    /** 다시 굴릴 수 있는 결과표 선택지를 index 자리에 넣었다 */
    public void setChaosSlot(int index) { chaosSlot = index; }

    /** 이 이벤트의 주사위를 끝낸다 */
    public void finishChaos() {
        chaosSlot = null;
        chaosDone = true;
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