package com.textdungeon.system;


import com.textdungeon.model.Job;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UserRecord {
    private int gem;
    private final Set<String> unlockJobName;
    private final Set<String> unlockTraitName;
    private final Map<String, Integer> upgradeLevels;
    private int killCount;
    private int clearCount;
    public UserRecord(){
        this.gem = 0;
        this.unlockJobName = new HashSet<>();
        this.unlockTraitName = new HashSet<>();
        this.upgradeLevels = new HashMap<>();
        for (Job job: Job.values()) {
            if (job.defaultUnlocked){
                unlockJobName.add(job.name);
            }
        }
        for (Job job : Job.values()) {
            if (job.defaultUnlocked) {
                unlockTraitName.add(job.trait.name());
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

    public void unlockTraits(String traitId){
        unlockTraitName.add(traitId);
    }
    public boolean isUnlockTrait(String traitId){
        return unlockTraitName.contains(traitId);
    }

    public Set<String> getUnlockJobName() {
        return unlockJobName;
    }
    public Map<String, Integer> getUpgradeLevels() {
        return upgradeLevels;
    }
    public Set<String> getUnlockTraitName() {
        return unlockTraitName;
    }

    public void addKillCount(){
        killCount++;
    }
    public int getKillCount() {
        return killCount;
    }

    public void addClearCount() {
        this.clearCount++;
    }
    public int getClearCount() {
        return clearCount;
    }
    public int getGem() { return gem; }
    public void deductGem(int gem) { this.gem -= gem; }
    public void addGem(int gem) { this.gem += gem; }

}
