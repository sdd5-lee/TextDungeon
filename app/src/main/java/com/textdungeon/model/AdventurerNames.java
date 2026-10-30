package com.textdungeon.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * 이름 정하기 창의 주사위. 직업에 어울리는 수식어 + 이름 (예: 전사 "강철의 브란").
 * 수식어는 6글자, 이름은 3글자까지라 띄어쓰기를 더해도 MAX_LENGTH를 넘지 않는다.
 */
public final class AdventurerNames {

    /** 이름 칸에 쓸 수 있는 최대 글자 수 (이벤트 화면 이름 자리에 맞춘 길이) */
    public static final int MAX_LENGTH = 10;

    private static final String[] NAMES = {
            "브란", "카엘", "로웬", "세라", "알릭", "모르간", "에이린", "타린", "볼크", "실바",
            "루카", "아델", "케른", "미라", "오린", "하르딘", "이그나", "베일", "도란", "리스"
    };

    private static final Map<Job, String[]> EPITHETS = new EnumMap<>(Job.class);

    static {
        EPITHETS.put(Job.WARRIOR, new String[]{"강철의", "피에 젖은", "꺾이지 않는"});
        EPITHETS.put(Job.MAGE, new String[]{"잿빛의", "별을 읽는", "푸른 불꽃의"});
        EPITHETS.put(Job.ROGUE, new String[]{"그림자", "소리 없는", "밤의"});
        EPITHETS.put(Job.ARCHER, new String[]{"매의 눈", "바람의", "숲의"});
        EPITHETS.put(Job.KNIGHT, new String[]{"맹세의", "철벽의", "은빛"});
        EPITHETS.put(Job.MONK, new String[]{"고요한", "맨주먹", "깨달은"});
        EPITHETS.put(Job.PALADIN, new String[]{"빛의", "서약한", "성스러운"});
        EPITHETS.put(Job.WARLOCK, new String[]{"계약한", "검은 칼날의", "마검의"});
        EPITHETS.put(Job.HERO, new String[]{"예언된", "운명의", "별의 아이"});
    }

    private AdventurerNames() {
    }

    public static String random(Job job, Random rnd) {
        String[] epithets = EPITHETS.get(job);
        String name = NAMES[rnd.nextInt(NAMES.length)];
        if (epithets == null) return name;
        return epithets[rnd.nextInt(epithets.length)] + " " + name;
    }

    /** 테스트용: 이 직업이 쓸 수 있는 수식어 */
    static String[] epithetsOf(Job job) {
        return EPITHETS.get(job);
    }
}
