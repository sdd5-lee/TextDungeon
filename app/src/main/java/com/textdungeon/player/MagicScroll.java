package com.textdungeon.player;

import com.textdungeon.model.LearnedMagic;
import com.textdungeon.model.Magic;
import com.textdungeon.model.MagicSchool;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MagicScroll {
    /** 지혜가 이만큼 오를 때마다 코어 포인트 1개 (20, 40, 60...) */
    public static final int WISDOM_PER_CORE = 20;

    /** 마법서에서 마법 하나의 상태. 막힌 곳은 계열 → 이전 마법 → 지혜 순으로 본다 */
    public enum LearnState { LEARNED, CAN_LEARN, SCHOOL_LOCKED, NEED_PREVIOUS, NEED_WISDOM }

    private List<LearnedMagic> learnedMagicList;
    /** 코어 포인트로 연 계열 (이번 판에서만 유효) */
    private Set<MagicSchool> openedSchools = new HashSet<>();
    /** 시작할 때 받은 코어 (마법사 계열 직업은 1개) */
    private int startCores;
    /** 이번 판에서 도달한 가장 높은 지혜. 지혜가 떨어져도 이미 얻은 코어는 빼앗지 않는다 */
    private int bestWisdom;
    /** 알림을 띄운 코어 수 (새로 얻은 코어만 알리려고) */
    private int notifiedCores;

    public MagicScroll() {
        learnedMagicList = new ArrayList<>();
    }

    /** 새 판 시작. 시작 코어는 알릴 필요가 없으니 알린 것으로 친다 */
    public void startRun(int startCores, int wisdom) {
        this.startCores = startCores;
        this.notifiedCores = startCores;
        this.bestWisdom = wisdom;
    }

    public void recordWisdom(int wisdom) {
        bestWisdom = Math.max(bestWisdom, wisdom);
    }

    public int getTotalCores(int wisdom) {
        return startCores + Math.max(bestWisdom, wisdom) / WISDOM_PER_CORE;
    }

    public int getAvailableCores(int wisdom) {
        return Math.max(0, getTotalCores(wisdom) - openedSchools.size());
    }

    /** 다음 코어를 받는 지혜 */
    public int getNextCoreWisdom(int wisdom) {
        return (Math.max(bestWisdom, wisdom) / WISDOM_PER_CORE + 1) * WISDOM_PER_CORE;
    }

    public boolean isSchoolOpen(MagicSchool school) {
        return !school.needsCore() || openedSchools.contains(school);
    }

    /** 코어 1개를 써서 계열을 연다. 이미 열렸거나 코어가 없으면 false */
    public boolean openSchool(MagicSchool school, int wisdom) {
        if (isSchoolOpen(school) || getAvailableCores(wisdom) <= 0) return false;
        openedSchools.add(school);
        return true;
    }

    /** 아직 알리지 않은 새 코어 수를 돌려주고 알린 것으로 표시한다 */
    public int takeNewCores(int wisdom) {
        int total = getTotalCores(wisdom);
        int fresh = total - notifiedCores;
        if (fresh <= 0) return 0;
        notifiedCores = total;
        return fresh;
    }

    public LearnState getLearnState(Magic magic, int wisdom) {
        if (hasMagic(magic.getId())) return LearnState.LEARNED;
        if (!isSchoolOpen(magic.getSchool())) return LearnState.SCHOOL_LOCKED;
        if (magic.getRequires() != null && !hasMagic(magic.getRequires())) return LearnState.NEED_PREVIOUS;
        if (wisdom < magic.getValue()) return LearnState.NEED_WISDOM;
        return LearnState.CAN_LEARN;
    }
    public void addMagic(String magicId, int maxCount) {
        if (!hasMagic(magicId)) {
            learnedMagicList.add(new LearnedMagic(magicId, maxCount));
        }
    }
    public boolean hasMagic(String magicId) {
        return learnedMagicList.stream().anyMatch(m -> m.getMagicId().equals(magicId));
    }
    public LearnedMagic getMagic(String magicId) {
        for (LearnedMagic lm : learnedMagicList) {
            if (lm.getMagicId().equals(magicId)) return lm;
        }
        return null;
    }
    public void restoreAll() {
        for (LearnedMagic lm : learnedMagicList) lm.restore();
    }

    public void updateCounts(int wisdom) {
        int addCount = 1 + (wisdom / 2);
        for (LearnedMagic lm : learnedMagicList) {
            lm.setCurrentCount(addCount);
            lm.setMaxCount(addCount);
        }
    }
    /**
     * 지혜가 레벨업 없이 바뀌었을 때, 그 변화량만큼만 마법 최대 횟수를 조정한다.
     * updateCounts()와 달리 이미 쓴 횟수를 다시 채우지 않고, 마법마다 다른 기본 횟수도 유지한다.
     */
    public void adjustForWisdomChange(int oldWisdom, int newWisdom) {
        int delta = (1 + newWisdom / 2) - (1 + oldWisdom / 2);
        if (delta == 0) return;
        for (LearnedMagic lm : learnedMagicList) {
            int newMax = Math.max(1, lm.getMaxCount() + delta);
            int newCurrent = lm.getCurrentCount() + Math.max(0, delta);
            lm.setMaxCount(newMax);
            lm.setCurrentCount(Math.max(0, Math.min(newMax, newCurrent)));
        }
    }
    public void removeMagic(String magicId){
        learnedMagicList.removeIf(l -> l.getMagicId().equals(magicId));
    }
    public List<LearnedMagic> getLearnedMagics() {
        return learnedMagicList;
    }
}