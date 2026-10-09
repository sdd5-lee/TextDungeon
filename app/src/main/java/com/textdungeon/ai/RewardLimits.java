package com.textdungeon.ai;

import com.textdungeon.model.RewardStat;

import java.util.ArrayList;
import java.util.List;

/**
 * AI가 만든 보상 수치의 층별 상한/하한.
 * 혼돈의 주사위와 AI 이벤트 생성에서 같이 쓴다.
 *
 * 기준은 event_list.json의 기존 이벤트 수치 (예: 31~40층 경험치 최대 900, 골드 최대 1500, 기본 스탯 +30).
 * 기존 이벤트보다 조금 넉넉하게 잡되, 젬 정산이나 스탯을 한 번에 망가뜨릴 수준(9999 등)은 막는다.
 * 난이도 보상 배수는 이 값에 곱해서 적용된다.
 */
public final class RewardLimits {
    private RewardLimits() {}

    /**
     * @return 층에 맞게 잘라낸 값. 0이면 그 스탯 보상은 버린다.
     */
    public static int clamp(String type, int value, int floor) {
        int f = Math.max(1, floor);
        switch (type) {
            case "힘":
            case "민첩":
            case "체력":
            case "지혜": {
                int max = 5 + f;               // 1층 6, 40층 45
                int min = -(2 + f / 4);        // 1층 -2, 40층 -12
                return clampRange(value, min, max);
            }
            case "경험치":
                return clampRange(value, 0, 100 + f * 20);          // 40층 900
            case "골드":
                return clampRange(value, -(50 + f * 10), 150 + f * 35); // 40층 -450 ~ 1550
            case "회복":
                return clampRange(Math.abs(value), 0, 50 + f * 5);      // 40층 250
            case "데미지":
                return clampRange(Math.abs(value), 0, 20 + f * 3);      // 40층 140
            default:
                return 0;
        }
    }

    /** 리스트 전체에 clamp를 적용하고, 0이 되거나 모르는 종류는 뺀 새 리스트를 돌려준다. */
    public static List<RewardStat> clampAll(List<RewardStat> stats, int floor) {
        List<RewardStat> result = new ArrayList<>();
        if (stats == null) return result;
        for (RewardStat s : stats) {
            if (s == null || s.getStatType() == null) continue;
            int v = clamp(s.getStatType(), s.getValue(), floor);
            if (v != 0) result.add(new RewardStat(s.getStatType(), v));
        }
        return result;
    }

    private static int clampRange(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
