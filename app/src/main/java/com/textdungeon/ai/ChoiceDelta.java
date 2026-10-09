package com.textdungeon.ai;

import com.textdungeon.model.Reward;
import com.textdungeon.model.RewardStat;

import java.util.Collections;
import java.util.List;

/**
 * 혼돈의 주사위가 AI에게서 받아오는 "추가분".
 * 이벤트 전체가 아니라 새 선택지 1개 + 그 보상만 담는다.
 */
public class ChoiceDelta {
    private final String choiceText;
    private final String rewardDescription;
    private final String itemId;          // 없으면 null
    private final List<RewardStat> stats; // 없으면 빈 리스트

    public ChoiceDelta(String choiceText, String rewardDescription, String itemId, List<RewardStat> stats) {
        this.choiceText = choiceText;
        this.rewardDescription = rewardDescription;
        this.itemId = itemId;
        this.stats = stats != null ? stats : Collections.emptyList();
    }

    public String getChoiceText() { return choiceText; }
    public String getRewardDescription() { return rewardDescription; }
    public String getItemId() { return itemId; }
    public List<RewardStat> getStats() { return stats; }

    public Reward toReward() {
        return new Reward(rewardDescription, itemId, stats);
    }
}
