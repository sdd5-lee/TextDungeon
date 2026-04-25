package com.textdungeon.system;


import com.textdungeon.model.Job;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UserRecord {
    private int score;
    private final Set<String> unlockJobName;
    private final Map<String, Integer> upgradeLevels;
    public UserRecord(){
        this.score = 0;
        this.unlockJobName = new HashSet<>();
        this.upgradeLevels = new HashMap<>();
        for (Job job: Job.values()) {
            if (job.defaultUnlokced){
                unlockJobName.add(job.name);
            }
        }
    }
    public void unlockJobs(String jobName){
        unlockJobName.add(jobName);
    }
    public boolean isUnlockJob(String jobName){
        return unlockJobName.contains(jobName);
    }

    public int getUpgradeLevel(String upgradeId) {
        return upgradeLevels.getOrDefault(upgradeId, 0);
    }

    public void levelUpUpgrade(String upgradeId) {
        int currentLevel = getUpgradeLevel(upgradeId);
        upgradeLevels.put(upgradeId, currentLevel + 1);
    }
    public int getScore() { return score; }
    public void deductScore(int score) { this.score -= score; }
    public void addScore(int score) { this.score += score; }

    public Set<String> getUnlockJobName() {
        return unlockJobName;
    }

    public Map<String, Integer> getUpgradeLevels() {
        return upgradeLevels;
    }
}
