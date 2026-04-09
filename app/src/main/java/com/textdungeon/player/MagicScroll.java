package com.textdungeon.player;

import com.textdungeon.model.LearnedMagic;

import java.util.ArrayList;
import java.util.List;

public class MagicScroll {
    private List<LearnedMagic> learnedMagicList;

    public MagicScroll() {
        learnedMagicList = new ArrayList<>();
    }

    public List<LearnedMagic> getLearnedMagicList() {
        return learnedMagicList;
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
}
