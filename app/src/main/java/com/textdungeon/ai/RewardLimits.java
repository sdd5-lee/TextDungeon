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
 * 던전 보상 배수는 이 값에 곱해서 적용된다.
 */
public final class RewardLimits {
    private RewardLimits() {}

    /**
     * @return 층에 맞게 잘라낸 값. 0이면 그 스탯 보상은 버린다.
     */
    public static int clamp(String type, int value, int floor) {
        int f = Math.max(1, floor);
        if (isMagnitudeOnly(type)) return clampRange(Math.abs(value), 0, max(type, f));
        return clampRange(value, min(type, f), max(type, f));
    }

    /**
     * 혼돈의 주사위 결과표(ChaosTable)용. 층별 상한의 percent%만큼, 음수면 하한(깎는 쪽)의 percent%만큼.
     * 회복·데미지는 크기만 본다.
     *
     * @param limitPercent percent를 몇 %까지 허용할지 (일반 선택지 100, 특별 선택지 200)
     * @return 0이면 그 스탯 보상은 버린다 (모르는 종류, 깎을 수 없는 종류의 음수 등)
     */
    public static int scaled(String type, int percent, int floor, int limitPercent) {
        int f = Math.max(1, floor);
        int p = isMagnitudeOnly(type) ? Math.abs(percent) : percent;
        p = clampRange(p, -limitPercent, limitPercent);
        int base = p >= 0 ? max(type, f) : -min(type, f);
        return (int) Math.round(base * p / 100.0);
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

    /** 좋은 쪽 상한. 회복·데미지는 크기 상한. 모르는 종류는 0 */
    private static int max(String type, int f) {
        switch (type) {
            case "힘":
            case "민첩":
            case "체력":
            case "지혜":
                return 5 + f;           // 1층 6, 40층 45
            case "경험치":
                return 100 + f * 20;    // 40층 900
            case "골드":
                return 150 + f * 35;    // 40층 1550
            case "회복":
                return 50 + f * 5;      // 40층 250
            case "데미지":
                return 20 + f * 3;      // 40층 140
            default:
                return 0;
        }
    }

    /** 깎는 쪽 하한(0 이하). 깎을 수 없는 종류는 0 */
    private static int min(String type, int f) {
        switch (type) {
            case "힘":
            case "민첩":
            case "체력":
            case "지혜":
                return -(2 + f / 4);    // 1층 -2, 40층 -12
            case "골드":
                return -(50 + f * 10);  // 40층 -450
            default:
                return 0;
        }
    }

    private static boolean isMagnitudeOnly(String type) {
        return "회복".equals(type) || "데미지".equals(type);
    }

    private static int clampRange(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
