package com.textdungeon.model;

import java.util.Set;

/**
 * 던전 하나 (dungeon_list.json). 예전의 쉬움·보통·어려움 대신 던전을 고른다.
 * 모든 던전은 같은 층별 몬스터·이벤트를 쓰고, 클리어하는 층과 몬스터·보상 배수가 다르다.
 * 이벤트에 dungeons를 적으면 그 던전에서만 나온다.
 */
public class Dungeon {
    private String id;
    private String name;
    private String description;
    private String imgId;
    /** 이 층을 넘기면 클리어 */
    private int clearFloor;
    /** 먼저 클리어해야 열리는 던전 id (없으면 처음부터 열림) */
    private String requires;
    /** 신들의 이벤트(AI) 수. 1, 21, 41, 61층 순서로 나온다 */
    private int aiEvents;
    /** 몬스터 체력·공격력 배수 */
    private int statMultiplier = 1;
    /** 이벤트 보상을 몇 번 받는지 */
    private int rewardMultiplier = 1;
    /** 클리어 보상 젬 (기본 정산에 더한다) */
    private int clearGems;
    /** 마지막 층에 반드시 나오는 이벤트 (그 던전의 보스) */
    private String bossEventId;
    /** 마지막 층을 넘었을 때 나오는 글 */
    private String clearText;

    public Dungeon() {}

    /** 테스트용 */
    public Dungeon(String id, String requires, int clearFloor, int statMultiplier, int rewardMultiplier) {
        this.id = id;
        this.requires = requires;
        this.clearFloor = clearFloor;
        this.statMultiplier = statMultiplier;
        this.rewardMultiplier = rewardMultiplier;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getImgId() { return imgId; }
    public int getClearFloor() { return clearFloor; }
    public String getRequires() { return requires; }
    public int getAiEvents() { return aiEvents; }
    public int getStatMultiplier() { return Math.max(1, statMultiplier); }
    public int getRewardMultiplier() { return Math.max(1, rewardMultiplier); }
    public int getClearGems() { return clearGems; }
    public String getBossEventId() { return bossEventId; }
    public String getClearText() { return clearText; }

    /** 앞 던전을 클리어했거나 앞 던전이 없으면 열려 있다 */
    public boolean isOpen(Set<String> clearedDungeons) {
        return requires == null || (clearedDungeons != null && clearedDungeons.contains(requires));
    }

    /** AI 이벤트가 나오는 층 (0번째 = 1층, 1번째 = 21층 ...) */
    public static int aiEventFloor(int index) {
        return index * 20 + 1;
    }
}
