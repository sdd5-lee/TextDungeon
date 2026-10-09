package com.textdungeon.data;

import android.content.Context;

import com.textdungeon.ai.AiManager;
import com.textdungeon.event.GameEvent;
import com.textdungeon.model.Item;
import com.textdungeon.model.Monster;
import com.textdungeon.model.Job;
import com.textdungeon.model.Magic;
import com.textdungeon.player.Player;
import com.textdungeon.system.AchievementManager;
import com.textdungeon.system.GameSave;
import com.textdungeon.system.UserRecord;

import java.util.HashMap;
import java.util.Map;

public class DataControlTower {
    private static DataControlTower instance;
    private final Context appContext;
    private DataControl<Monster> monsterManager;
    private DataControl<Item> itemManager;
    private DataControl<GameEvent> eventManager;
    private DataControl<Magic> magicManager;
    private Player player;
    private UserRecord userRecord;
    private DungeonControl dungeonControl;
    private AiManager aiManager;
    private Difficulty difficulty;
    private Map<Integer, GameEvent> aiEvents;
    private AchievementManager achievementManager;

    private DataControlTower(Context context){
        this.appContext = context.getApplicationContext();
        this.aiManager = new AiManager();
        this.aiEvents = new HashMap<>();

        loadGameData();

        this.achievementManager = new AchievementManager(appContext);
        if (this.userRecord != null) {
            this.achievementManager.syncSavedData(this.userRecord.getAchievements());
        }

        initAll(context);
    }

    public static DataControlTower getInstance(Context context){
        if (instance == null){
            instance = new DataControlTower(context.getApplicationContext());
        }
        return instance;
    }

    private void initAll(Context context) {
        monsterManager = new DataControl<>(Monster.class);
        monsterManager.init(context, "monster_list.json");

        itemManager = new DataControl<>(Item.class);
        itemManager.init(context, "item_list.json");

        eventManager = new DataControl<>(GameEvent.class, EventGson.create());
        eventManager.init(context, "event_list.json");

        magicManager = new DataControl<>(Magic.class);
        magicManager.init(context, "magic_list.json");

        // 무결성 검사
        boolean valid = DataValidator.validateAll(
                monsterManager.getAll(),
                itemManager.getAll(),
                eventManager.getAll()
        );

        if (!valid) {
            throw new IllegalStateException("게임 데이터 무결성 검사 실패");
        }
    }

    private void loadGameData() {
        this.userRecord = GameSave.loadUserRecord(appContext);
        if (this.userRecord == null){
            this.userRecord = new UserRecord();
        }
        this.dungeonControl = new DungeonControl();
        GameSave save = GameSave.runLoad(appContext);
        if (save != null){
            this.player = save.getPlayer();
            this.difficulty = save.getDifficulty();
            this.dungeonControl.setCurrentFloor(save.getCurrentFloor());
            // 예전엔 저장만 하고 불러오지 않아서 이어하기 하면 AI 이벤트가 사라졌다
            if (save.getAiEvents() != null) {
                this.aiEvents.putAll(save.getAiEvents());
            }
            this.dungeonControl.setCurrentEvent(save.getCurrentEvent());
        } else {
            this.player = null;
            this.difficulty = Difficulty.NORMAL;
            this.dungeonControl.setCurrentFloor(1);
        }
    }

    public void startNewGame(String name, Job job, String traitId){
        this.player = GameSave.createNewPlayer(this.userRecord, name, job, traitId);
        this.player.setTraitId(traitId);
        // createNewPlayer는 직업 기본 특성 기준으로 HP를 채운다. 다른 특성을 고르면(예: 마력폭주 ↔ 다른 특성)
        // 최대 HP가 달라져 시작 HP가 가득 차지 않으므로 선택한 특성 기준으로 다시 채운다 (완벽주의자 특성 등에 영향)
        this.player.getStat().setHp(this.player.getMaxHp());

        // 이전 판의 AI 이벤트/현재 이벤트가 새 판에 섞이지 않도록 정리
        this.aiEvents.clear();
        this.dungeonControl.setCurrentEvent(null);

        if (this.player.getTrait() != null) {
            if (this.player.getTrait().triggerTreasure()) {
                for (int i = 0; i < 3; i++) {
                    giveRandomStartingItem(this.player);
                }
            }
        }
        this.dungeonControl.setCurrentFloor(1);
        saveGame();
    }

    public void saveGame() {
        if (this.player == null){ return; }
        if (this.userRecord != null && this.achievementManager != null) {
            this.userRecord.setAchievements(this.achievementManager.getAllAchievements());
            GameSave.saveUserRecord(appContext, this.userRecord);
        }

        GameSave currentSave = new GameSave(this.player, dungeonControl.getCurrentFloor(), difficulty,
                aiEvents, dungeonControl.getCurrentEvent());
        currentSave.runSave(appContext);
    }

    public void resetRun() {
        this.player = null;
        getDungeonControl().setCurrentEvent(null);
        this.aiEvents.clear();
        GameSave.deleteRun(appContext);
    }

    public AchievementManager getAchievementManager() {
        return achievementManager;
    }

    public Context getAppContext() { return appContext; }
    public DataControl<GameEvent> getEventManager() { return eventManager; }
    public DataControl<Item> getItemManager() { return itemManager; }
    public DataControl<Magic> getMagicManager() { return magicManager; }
    public DataControl<Monster> getMonsterManager() { return monsterManager; }
    public Player getPlayer() { return player; }
    public UserRecord getUserRecord() { return userRecord; }
    public DungeonControl getDungeonControl() { return dungeonControl; }
    public void setPlayer(Player player) { this.player = player; }
    public AiManager getAiManager() { return aiManager; }
    public void setUserRecord(UserRecord userRecord) { this.userRecord = userRecord; }
    public void setDifficulty(String difficultyName) { difficulty = Difficulty.valueOf(difficultyName); }
    public Difficulty getDifficulty() { return difficulty; }
    public Map<Integer, GameEvent> getAiEvents() { return aiEvents; }

    /**
     * 40층 이상 이벤트(보상, 상점)에서 나오는 장비 목록. 보물의 신 이벤트에서 보상 후보로 쓴다.
     * 아이템 데이터에는 층 정보가 없으므로 기존 이벤트 데이터로 판단한다. 포션 같은 소모품은 제외.
     */
    public java.util.List<Item> getDeepFloorGear() {
        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
        for (GameEvent e : eventManager.getAll()) {
            if (e.getMinFloor() < 40) continue;
            if (e.getRewards() != null) {
                for (com.textdungeon.model.Reward r : e.getRewards()) {
                    if (r != null && r.getItemId() != null) ids.add(r.getItemId());
                }
            }
            if (e instanceof com.textdungeon.event.ShopEvent) {
                java.util.List<String> shop = ((com.textdungeon.event.ShopEvent) e).getShopItemIds();
                if (shop != null) ids.addAll(shop);
            }
        }
        java.util.List<Item> result = new java.util.ArrayList<>();
        for (String id : ids) {
            Item item = itemManager.spawn(id);
            if (item != null && !"consumables".equals(item.getType())) result.add(item);
        }
        return result.isEmpty() ? itemManager.getAll() : result;
    }

    /**
     * 해당 층 범위에 실제로 등장하는 몬스터 목록.
     * 몬스터 데이터에는 층 정보가 없으므로, 기존 전투 이벤트의 층 범위(minFloor~maxFloor)로 판단한다.
     * AI 이벤트 생성 때 이 목록만 넘겨서 1층에 보스급 몬스터가 나오는 일을 막는다.
     */
    public java.util.List<Monster> getMonstersForFloor(int fromFloor, int toFloor) {
        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
        for (GameEvent e : eventManager.getAll()) {
            if (!(e instanceof com.textdungeon.event.BattleEvent)) continue;
            String enemyId = ((com.textdungeon.event.BattleEvent) e).getEnemyId();
            boolean overlaps = e.getMinFloor() <= toFloor && e.getMaxFloor() >= fromFloor;
            if (overlaps && enemyId != null) ids.add(enemyId);
        }
        java.util.List<Monster> result = new java.util.ArrayList<>();
        for (String id : ids) {
            Monster m = monsterManager.spawn(id);
            if (m != null) result.add(m);
        }
        // 해당 층 범위에 전투 이벤트가 하나도 없으면 전체 몬스터로 대체
        return result.isEmpty() ? monsterManager.getAll() : result;
    }
    public void addAiEvent(int targetFloor, GameEvent newEvent) { aiEvents.put(targetFloor, newEvent); }

    private void giveRandomStartingItem(Player p) {
        Item randomItem = itemManager.getRandomData();
        if (randomItem != null) {
            p.getInventory().addItem(randomItem);
        }
    }
}