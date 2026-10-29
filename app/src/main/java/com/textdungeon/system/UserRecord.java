package com.textdungeon.system;


import com.textdungeon.model.Achievement;
import com.textdungeon.model.Job;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserRecord {
    private int gem;
    private final Set<String> unlockJobName;
    private final Set<String> unlockTraitName;
    private final Map<String, Integer> upgradeLevels;
    private int killCount;
    private int clearCount;
    private List<Achievement> achievements;
    private Set<String> discoveredItems;
    private Set<String> discoveredMonsters;
    private Set<String> discoveredEvents;

    // ===== 결제 =====
    // Play에서 마지막으로 확인한 보유 상품 ID(Product.id). 오프라인일 때 쓰는 캐시이고,
    // 동기화가 성공할 때마다 Play 목록으로 통째로 덮어쓴다.
    // Gson 역직렬화 때문에 final로 두지 않는다. 예전 세이브에는 없으므로 null일 수 있다
    private Set<String> ownedProducts;
    private long entitlementSyncedAt;
    // 마지막으로 고른 시나리오 ID. null이면 기본 시나리오
    private String lastScenarioId;

    public UserRecord(){
        this.gem = 0;
        this.unlockJobName = new HashSet<>();
        this.unlockTraitName = new HashSet<>();
        this.upgradeLevels = new HashMap<>();

        this.discoveredItems = new HashSet<>();
        this.discoveredMonsters = new HashSet<>();
        this.discoveredEvents = new HashSet<>();
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
    public List<Achievement> getAchievements() { return achievements; }
    public void setAchievements(List<Achievement> achievements) { this.achievements = achievements; }
    public boolean discoverItem(String itemId) {
        return discoveredItems.add(itemId);
    }
    public int getDiscoveredItemsCount() {
        return discoveredItems.size();
    }
    public Set<String> getDiscoveredItems() {
        if (discoveredItems == null) return new HashSet<>();
        return discoveredItems;
    }

    public boolean discoverMonster(String monsterId) {
        return discoveredMonsters.add(monsterId);
    }
    public int getDiscoveredMonstersCount() {
        return discoveredMonsters.size();
    }
    public Set<String> getDiscoveredMonsters() {
        if (discoveredMonsters == null) return new HashSet<>();
        return discoveredMonsters;
    }

    public boolean discoverEvent(String eventId) {
        return discoveredEvents.add(eventId);
    }
    public int getDiscoveredEventsCount() {
        return discoveredEvents.size();
    }
    public Set<String> getDiscoveredEvents() {
        if (discoveredEvents == null) return new HashSet<>();
        return discoveredEvents;
    }
    public Set<String> getOwnedProducts() {
        if (ownedProducts == null) ownedProducts = new HashSet<>();
        return ownedProducts;
    }
    public void setOwnedProducts(Set<String> ids) {
        ownedProducts = new HashSet<>(ids);
        entitlementSyncedAt = System.currentTimeMillis();
    }
    public long getEntitlementSyncedAt() { return entitlementSyncedAt; }

    public String getLastScenarioId() { return lastScenarioId; }
    public void setLastScenarioId(String lastScenarioId) { this.lastScenarioId = lastScenarioId; }

    /** 기록 초기화. 유료 상품(ownedProducts)은 Play가 기준이므로 지우지 않는다 */
    public void clearAll() {
        this.gem = 0;
        this.killCount = 0;
        this.clearCount = 0;

        this.upgradeLevels.clear();
        this.discoveredItems.clear();
        this.discoveredMonsters.clear();
        this.discoveredEvents.clear();

        if (this.achievements != null) {
            this.achievements.clear();
        }

        this.unlockJobName.clear();
        this.unlockTraitName.clear();

        for (Job job : Job.values()) {
            if (job.defaultUnlocked) {
                this.unlockJobName.add(job.name);
                this.unlockTraitName.add(job.trait.name());
            }
        }
    }
}
