package com.textdungeon.data;

public enum Difficulty {
    // 순서: 이름, 이벤트 생성 숫자, 몬스터 스탯 배수, 보상 획득량 배수
    EASY("쉬움", 1, 1, 1),
    NORMAL("보통", 2, 2, 2),
    HARD("어려움", 3, 3, 3);

    public final String displayName;
    public final int eventCount;
    public final int statMultiplier;
    public final int rewardMultiplier;

    Difficulty(String displayName, int eventCount, int statMultiplier, int rewardMultiplier) {
        this.displayName = displayName;
        this.eventCount = eventCount;
        this.statMultiplier = statMultiplier;
        this.rewardMultiplier = rewardMultiplier;
    }
}
