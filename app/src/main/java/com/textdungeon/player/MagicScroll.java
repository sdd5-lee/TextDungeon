package com.textdungeon.player;

import com.textdungeon.model.LearnedMagic;

import java.util.ArrayList;
import java.util.List;

public class MagicScroll {
    private List<LearnedMagic> learnedMagicList;

    public MagicScroll() {
        learnedMagicList = new ArrayList<>();
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