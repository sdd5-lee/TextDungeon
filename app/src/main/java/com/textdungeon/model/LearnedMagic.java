package com.textdungeon.model;

public class LearnedMagic {
    private String magicId;
    private int currentCount;
    private int maxCount;

    public LearnedMagic(String magicId, int maxCount) {
        this.magicId = magicId;
        this.maxCount = maxCount;
        this.currentCount = maxCount;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }
    public void setCurrentCount(int currentCount) {
        this.currentCount = currentCount;
    }

    public String getMagicId() {
        return magicId;
    }

    public void restore() {
        currentCount = maxCount;
    }
    public boolean use() {
        if (currentCount > 0) {
            currentCount--;
            return true;
        }
        return false;
    }
}