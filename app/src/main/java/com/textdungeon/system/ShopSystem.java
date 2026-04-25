package com.textdungeon.system;

import com.textdungeon.data.DataControlTower;
import com.textdungeon.model.Job;
import com.textdungeon.model.ShopUpgrade;

public class ShopSystem {
    DataControlTower dt;
    public ShopSystem(android.content.Context context){
        this.dt = DataControlTower.getInstance(context);
    }
    private int getJobPrice(Job job) {
        switch (job) {
            case HERO: return 5000;
            //case ARCHER: return 1500;
            default: return 0;
        }
    }
    public String buyJob(String jobName){
        Job selectJob;
        try {
            selectJob = Job.valueOf(jobName);
        }catch (IllegalArgumentException e){
            return "존재하지 않는 직업 코드입니다.";
        }
        UserRecord record = dt.getUserRecord();
        if(record.isUnlockJob(selectJob.name)){
            return "직업 : ["+ selectJob.name + "]은 이미 해금한 직업입니다";
        }
        int price = getJobPrice(selectJob);
        if (record.getScore() >= price) {
            record.deductScore(price);
            dt.saveGame();
            return "직업 : ["+ selectJob.name + "]이 해금되었습니다";
        }else {
            return "구매 실패 [재화가 부족합니다]";
        }
    }
    public String buyUpgrade(String upgradeId) {
        ShopUpgrade selectUpgrade;
        try {
            selectUpgrade = ShopUpgrade.valueOf(upgradeId);
        } catch (IllegalArgumentException e) {
            return "존재하지 않는 업그레이드 코드입니다.";
        }

        UserRecord record = dt.getUserRecord();
        int currentLevel = record.getUpgradeLevel(selectUpgrade.name());
        if (currentLevel >= selectUpgrade.maxLevel) {
            return "[" + selectUpgrade.title + "] 항목은 이미 최대 레벨(Lv." + selectUpgrade.maxLevel + ")입니다.";
        }
        int price = selectUpgrade.getNextPrice(currentLevel);

        if (record.getScore() >= price) {
            record.deductScore(price);
            record.levelUpUpgrade(selectUpgrade.name());
            dt.saveGame();

            int newLevel = record.getUpgradeLevel(selectUpgrade.name());
            return "[" + selectUpgrade.title + "] 레벨업 성공! (현재 Lv." + newLevel + ") / 남은 재화: " + record.getScore();
        } else {
            return "구매 실패 [재화가 부족합니다. 필요 재화: " + price + "]";
        }
    }

}
