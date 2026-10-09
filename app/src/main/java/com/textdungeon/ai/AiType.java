package com.textdungeon.ai;

public enum AiType {
    CHAOS(
            "혼돈의 신",
            "event_chaos_",
            "두 가지 선택지는 도무지 예측할 수 없는 기괴하고 혼란스러운 내용이어야 한다. 과업이 극도로 힘들거나 허무할 정도로 쉬운 일, 보상이 아주 작거나 파격적으로 큰 대가가 완전히 무작위(랜덤)로 뒤섞여서 주어지도록 하라."
    ),
    ADVENTURE(
            "모험의 신",
            "event_adventure_",
            "두 가지 선택지는 정통 로그라이크의 '하이리스크 하이리턴' 정체성을 담아야 한다. 선택지가 유저를 치명적인 위험에 노출시킬수록, 그 리스크에 완벽히 비례하는 엄청나고 거대한 보상을 설계하라."
    ),
    DEATH(
            "죽음의 신",
            "event_death_",
            "두 가지 선택지는 생명을 제물로 바치는 어두운 거래여야 한다. 플레이어의 현재 체력(HP)을 대량으로 파괴하거나 영구적인 대가를 요구하는 대신, 죽음의 문턱에서만 얻을 수 있는 강력하고 금기된 보상을 연동하라."
    ),
    STRUGGLE(
            "투쟁의 신",
            "event_struggle_",
            "이 이벤트의 type은 반드시 'battle'이어야 한다. 매우 강력하고 물리치기 어려운 몬스터(enemyId 필수 지정)와의 강제 전투를 발생시켜라. 유저가 이 투쟁에서 살아남아 승리할 경우, 보상은 아이템보다 플레이어의 스탯(힘, 민첩, 체력, 지혜)을 영구적으로 크게 올려주는 보상 위주로만 구성하라."
    ),
    TREASURE(
            "보물의 신",
            "event_treasure_",
            "이 이벤트의 type은 반드시 'normal'이고 shopItems는 null이어야 한다. 보물의 신이 심층 던전의 보물 두 개를 내밀고 플레이어는 그중 하나만 가질 수 있다. " +
            "choices는 각각 하나의 보물을 고르는 행동이고, 각 rewards의 itemId는 위 아이템 목록(심층 보물만 들어 있음)에서 서로 다른 id를 골라라. " +
            "rewards.description에는 어떤 보물을 얻었는지 묘사하고, statRewards는 비워 두거나 아주 작게 하라."
    );

    private final String godName;
    private final String idPrefix;
    private final String rule;

    AiType(String godName, String idPrefix, String rule) {
        this.godName = godName;
        this.idPrefix = idPrefix;
        this.rule = rule;
    }

    public String getGodName() { return godName; }
    public String getIdPrefix() { return idPrefix; }
    public String getRule() { return rule; }
}