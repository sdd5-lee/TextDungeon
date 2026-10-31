package com.textdungeon.system;

import com.textdungeon.ai.RewardLimits;
import com.textdungeon.model.Stat;
import com.textdungeon.player.Player;

/**
 * 시련의 신(마법·도적·힘·단련)이 여는 결투. 신마다 스탯 하나(지혜·민첩·힘·체력)를 시험한다.
 * - 플레이어의 그 스탯이 높을수록 적의 체력·공격력이 세진다 (스탯 3당 +1%, 최대 2배. 던전 배수 위에 곱한다)
 * - 이기면 그 스탯만 오른다 (스탯의 1/4 + 2, 층별 상한까지. 아이템·골드·경험치는 없다)
 * 이벤트는 던전에 들어갈 때 미리 만들어 두므로, 세기와 보상은 그 층에서 싸울 때의 스탯으로 정한다.
 */
public final class GodTrial {

    /** 적의 세기 상한 (%) */
    static final int MAX_ENEMY_PERCENT = 200;

    private GodTrial() {}

    /** 플레이어의 기본 스탯 (장비 제외) */
    public static int statOf(Player player, String stat) {
        Stat s = player.getStat();
        switch (stat) {
            case "힘": return s.getStrength();
            case "민첩": return s.getAgility();
            case "체력": return s.getHealth();
            case "지혜": return s.getWisdom();
            default: return 0;
        }
    }

    /** 적의 체력·공격력을 몇 %로 할지 */
    public static int enemyPercent(int statValue) {
        return Math.min(MAX_ENEMY_PERCENT, 100 + Math.max(0, statValue) / 3);
    }

    /** 이기면 오르는 양 (던전 보상 배수를 곱하기 전) */
    public static int reward(String stat, int statValue, int floor) {
        return RewardLimits.clamp(stat, 2 + Math.max(0, statValue) / 4, floor);
    }

    /** 이벤트 화면에 붙이는 한 줄. 예: "〔지혜의 시련〕 지금 지혜 60: 적이 20% 더 강하고, 이기면 지혜 +34" */
    public static String preview(String stat, int statValue, int floor, int rewardMultiplier) {
        int extra = enemyPercent(statValue) - 100;
        int gain = reward(stat, statValue, floor) * Math.max(1, rewardMultiplier);
        return "〔" + stat + "의 시련〕 지금 " + stat + " " + statValue + ": "
                + (extra > 0 ? "적이 " + extra + "% 더 강하고, " : "")
                + "이기면 " + stat + " +" + gain;
    }
}
