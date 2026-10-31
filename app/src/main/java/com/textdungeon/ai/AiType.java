package com.textdungeon.ai;

public enum AiType {
    CHAOS(
            "혼돈의 신",
            "event_chaos_",
            "기괴하고 예측할 수 없는 일. 허무할 만큼 쉽거나 터무니없이 어렵다",
            "두 가지 선택지는 도무지 예측할 수 없는 기괴하고 혼란스러운 내용이어야 한다. 과업이 극도로 힘들거나 허무할 정도로 쉬운 일, 보상이 아주 작거나 파격적으로 큰 대가가 완전히 무작위(랜덤)로 뒤섞여서 주어지도록 하라."
    ),
    ADVENTURE(
            "모험의 신",
            "event_adventure_",
            "위험을 무릅쓸수록 크게 얻는 모험",
            "두 가지 선택지는 정통 로그라이크의 '하이리스크 하이리턴' 정체성을 담아야 한다. 선택지가 유저를 치명적인 위험에 노출시킬수록, 그 리스크에 완벽히 비례하는 엄청나고 거대한 보상을 설계하라."
    ),
    DEATH(
            "죽음의 신",
            "event_death_",
            "생명을 제물로 바치는 어두운 거래",
            "두 가지 선택지는 생명을 제물로 바치는 어두운 거래여야 한다. 플레이어의 현재 체력(HP)을 대량으로 파괴하거나 영구적인 대가를 요구하는 대신, 죽음의 문턱에서만 얻을 수 있는 강력하고 금기된 보상을 연동하라."
    ),
    STRUGGLE(
            "투쟁의 신",
            "event_struggle_",
            "강한 적과 피할 수 없는 결투",
            "이 이벤트의 type은 반드시 'battle'이어야 한다. 매우 강력하고 물리치기 어려운 몬스터(enemyId 필수 지정)와의 강제 전투를 발생시켜라. 유저가 이 투쟁에서 살아남아 승리할 경우, 보상은 아이템보다 플레이어의 스탯(힘, 민첩, 체력, 지혜)을 영구적으로 크게 올려주는 보상 위주로만 구성하라."
    ),
    TREASURE(
            "보물의 신",
            "event_treasure_",
            "보물 둘 중 하나만 가질 수 있는 시험",
            "이 이벤트의 type은 반드시 'normal'이고 shopItems는 null이어야 한다. 보물의 신이 심층 던전의 보물 두 개를 내밀고 플레이어는 그중 하나만 가질 수 있다. " +
            "choices는 각각 하나의 보물을 고르는 행동이고, 각 rewards의 itemId는 위 아이템 목록(심층 보물만 들어 있음)에서 서로 다른 id를 골라라. " +
            "rewards.description에는 어떤 보물을 얻었는지 묘사하고, statRewards는 비워 두거나 아주 작게 하라."
    ),

    // ── 시련의 신: 플레이어의 그 스탯이 높을수록 결투가 어렵고 보상(그 스탯)도 크다 (GodTrial) ──
    MAGIC(
            "마법의 신",
            "event_magic_",
            "지혜를 시험하는 마법 결투. 지혜가 높을수록 상대가 강해지고 얻는 지혜도 크다",
            trialRule("마법의 신", "지혜"),
            "지혜"
    ),
    THIEF(
            "도적의 신",
            "event_thief_",
            "민첩을 시험하는 그림자 속 결투. 민첩이 높을수록 상대가 강해지고 얻는 민첩도 크다",
            trialRule("도적의 신", "민첩"),
            "민첩"
    ),
    STRENGTH(
            "힘의 신",
            "event_strength_",
            "힘을 시험하는 정면 승부. 힘이 높을수록 상대가 강해지고 얻는 힘도 크다",
            trialRule("힘의 신", "힘"),
            "힘"
    ),
    TRAINING(
            "단련의 신",
            "event_training_",
            "체력을 시험하는 끝없는 단련의 결투. 체력이 높을수록 상대가 강해지고 얻는 체력도 크다",
            trialRule("단련의 신", "체력"),
            "체력"
    );

    private final String godName;
    private final String idPrefix;
    // 틀 방식(TemplateEventGenerator) 프롬프트에 넣는 짧은 성격. rule은 JSON 방식(EventGenerator)용 지시문이다
    private final String tone;
    private final String rule;
    /** 시련의 신이 시험하고 보상으로 올려 주는 스탯 (힘, 민첩, 체력, 지혜). 다른 신은 null */
    private final String trialStat;

    AiType(String godName, String idPrefix, String tone, String rule) {
        this(godName, idPrefix, tone, rule, null);
    }

    AiType(String godName, String idPrefix, String tone, String rule, String trialStat) {
        this.godName = godName;
        this.idPrefix = idPrefix;
        this.tone = tone;
        this.rule = rule;
        this.trialStat = trialStat;
    }

    private static String trialRule(String godName, String stat) {
        return "이 이벤트의 type은 반드시 'battle'이어야 한다. " + godName + "이 플레이어의 " + stat
                + "을(를) 시험하는 결투를 연다(enemyId 필수 지정). 이기면 " + stat + "만 오르고 아이템·골드·경험치는 주지 않는다. "
                + "오르는 양과 적의 세기는 게임이 싸울 때의 " + stat + "에 맞춰 정하므로 statRewards는 비워 두어라.";
    }

    public String getGodName() { return godName; }
    public String getIdPrefix() { return idPrefix; }
    public String getTone() { return tone; }
    public String getRule() { return rule; }
    public String getTrialStat() { return trialStat; }
    public boolean isTrial() { return trialStat != null; }
}