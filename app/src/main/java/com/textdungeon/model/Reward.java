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
    /** 비어 있으면 null일 수 있다 */
    public List<RewardStat> getStatRewards() {
        return statRewards;
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
                    if ("경험치".equals(type) && expMultiplier(player) > 1) {
                        player.getStat().gainStatExp(value, expMultiplier(player));
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

    /** 경험치 보상에 곱해지는 특성 배수 (모험의 축복, 반신 혈통 = 2). apply()와 summary()가 같이 쓴다 */
    public static int expMultiplier(Player player) {
        Trait trait = player.getTrait();
        return trait != null && trait.modifyExp() > 1 ? trait.modifyExp() : 1;
    }

    /**
     * 실제로 적용되는 양으로 보상 요약을 만든다. 예: "(힘 +6, 경험치 +90, 낡은 활 ×2)". 적용할 것이 없으면 빈 문자열.
     * GameEvent.execute()가 보상 전체를 난이도 배수만큼 반복 적용하고, apply()가 경험치에 특성 배수를 곱하는 규칙을 따른다.
     *
     * @param rewardMultiplier 난이도 배수 (Difficulty.rewardMultiplier)
     * @param expMultiplier    특성 경험치 배수 (expMultiplier(player))
     * @param itemName         itemId 아이템의 이름. null이면 아이템은 요약에서 뺀다
     */
    public String summary(int rewardMultiplier, int expMultiplier, String itemName) {
        List<String> parts = new ArrayList<>();
        if (statRewards != null) {
            for (RewardStat s : statRewards) {
                int amount = s.getValue() * rewardMultiplier;
                String type = s.getStatType();
                if ("회복".equals(type)) {
                    parts.add("HP +" + amount);
                } else if ("데미지".equals(type)) {
                    parts.add("HP -" + amount);
                } else {
                    if ("경험치".equals(type)) amount *= expMultiplier;
                    parts.add(type + " " + (amount > 0 ? "+" : "") + amount);
                }
            }
        }
        if (itemId != null && itemName != null) {
            parts.add(rewardMultiplier > 1 ? itemName + " ×" + rewardMultiplier : itemName);
        }
        return parts.isEmpty() ? "" : "(" + String.join(", ", parts) + ")";
    }
}