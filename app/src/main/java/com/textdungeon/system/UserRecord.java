package com.textdungeon.system;


import com.textdungeon.model.Job;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UserRecord {
    private int  gem;
    private final Set<String> unlockJobName;
    private final Map<String, Integer> upgradeLevels;
    public UserRecord(){
        this.gem = 0;
        this.unlockJobName = new HashSet<>();
        this.upgradeLevels = new HashMap<>();
        for (Job job: Job.values()) {
            if (job.defaultUnlocked){
                unlockJobName.add(job.name);
            }
        }
    }
    public void unlockJobs(Job job){
        unlockJobName.add(job.name);
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
    public int getGem() { return gem; }
    public void deductScore(int score) { this.gem -= score; }
    public void addScore(int score) { this.gem += score; }

    public Set<String> getUnlockJobName() {
        return unlockJobName;
    }

    public Map<String, Integer> getUpgradeLevels() {
        return upgradeLevels;
    }
}
