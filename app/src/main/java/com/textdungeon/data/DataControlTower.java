package com.textdungeon.data;

import android.content.Context;

import com.textdungeon.ads.AdManager;
import com.textdungeon.ai.AiManager;
import com.textdungeon.ai.ChaosTable;
import com.textdungeon.billing.BillingManager;
import com.textdungeon.billing.Entitlements;
import com.textdungeon.billing.UserRecordEntitlementStore;
import com.textdungeon.event.GameEvent;
import com.textdungeon.model.Item;
import com.textdungeon.model.Monster;
import com.textdungeon.model.Job;
import com.textdungeon.model.Magic;
import com.textdungeon.player.Player;
import com.textdungeon.scenario.ScenarioPack;
import com.textdungeon.scenario.ScenarioRepository;
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
    private ChaosTable chaosTable = ChaosTable.EMPTY;
    private Player player;
    private UserRecord userRecord;
    private DungeonControl dungeonControl;
    private AiManager aiManager;
    private Difficulty difficulty;
    private Map<Integer, GameEvent> aiEvents;
    private AchievementManager achievementManager;
    private final BillingManager billingManager;
    private final AdManager adManager;
    private final ScenarioRepository scenarioRepository;
    /** 지금 읽혀 있는 시나리오 (진행 중인 판의 시나리오) */
    private ScenarioPack scenario = ScenarioPack.BASE;

    private DataControlTower(Context context){
        this.appContext = context.getApplicationContext();
        this.aiManager = new AiManager(appContext);
        this.aiEvents = new HashMap<>();

        loadGameData();

        // 결제·광고: 보유 상품은 UserRecord에 캐시한다. 둘 다 Activity를 붙잡지 않는다
        this.billingManager = new BillingManager(appContext,
                new UserRecordEntitlementStore(appContext, this::getUserRecord));
        this.adManager = new AdManager(appContext, this::getEntitlements);
        this.scenarioRepository = new ScenarioRepository();

        this.achievementManager = new AchievementManager(appContext);
        if (this.userRecord != null) {
            this.achievementManager.syncSavedData(this.userRecord.getAchievements());
        }

        // 이어하기면 그 판의 시나리오를, 아니면 기본 시나리오를 읽는다.
        // 환불로 잠긴 DLC여도 진행 중인 판은 끝까지 허용한다
        magicManager = new DataControl<>(Magic.class);
        magicManager.init(context, "magic_list.json"); // 마법은 모든 시나리오 공통
        loadScenario(scenario);
    }

    public static DataControlTower getInstance(Context context){
        if (instance == null){
            instance = new DataControlTower(context.getApplicationContext());
        }
        return instance;
    }

    /** 시나리오의 몬스터·아이템·이벤트를 읽는다. 경로는 ScenarioRepository에서만 받는다 */
    private void loadScenario(ScenarioPack pack) {
        monsterManager = new DataControl<>(Monster.class);
        monsterManager.init(appContext, scenarioRepository.assetPath(pack, ScenarioRepository.MONSTERS));

        itemManager = new DataControl<>(Item.class);
        itemManager.init(appContext, scenarioRepository.assetPath(pack, ScenarioRepository.ITEMS));

        eventManager = new DataControl<>(GameEvent.class, EventGson.create());
        eventManager.init(appContext, scenarioRepository.assetPath(pack, ScenarioRepository.EVENTS));
        chaosTable = ChaosTable.load(appContext, scenarioRepository.assetPath(pack, ScenarioRepository.CHAOS));
        this.scenario = pack;

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
            this.scenario = ScenarioPack.fromId(save.getScenarioId());
        } else {
            this.player = null;
            this.difficulty = Difficulty.NORMAL;
            this.dungeonControl.setCurrentFloor(1);
        }
    }

    public void startNewGame(String name, Job job, String traitId, ScenarioPack pack){
        // 시작 아이템(보물의 축복)도 고른 시나리오에서 나와야 하므로 플레이어를 만들기 전에 바꾼다
        if (pack != scenario) loadScenario(pack);
        userRecord.setLastScenarioId(pack.id);

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
                aiEvents, dungeonControl.getCurrentEvent(), scenario.id);
        currentSave.runSave(appContext);
    }

    public void resetRun() {
        this.player = null;
        getDungeonControl().setCurrentEvent(null);
        this.aiEvents.clear();
        GameSave.deleteRun(appContext);
        // 판이 끝났으니 온디바이스 모델(1~2GB)을 내린다. 다음 판을 시작할 때 다시 올린다
        aiManager.releaseOnDevice();
    }

    public AchievementManager getAchievementManager() {
        return achievementManager;
    }

    public Context getAppContext() { return appContext; }
    public BillingManager getBillingManager() { return billingManager; }
    public AdManager getAdManager() { return adManager; }
    public ScenarioRepository getScenarioRepository() { return scenarioRepository; }
    public ScenarioPack getScenario() { return scenario; }

    /** UserRecord 캐시로 새로 만든다. 지급 직후에도 최신 값을 돌려준다 */
    public Entitlements getEntitlements() {
        return new Entitlements(getUserRecord().getOwnedProducts());
    }
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
     * 포션 같은 소모품은 제외.
     */
    public java.util.List<Item> getDeepFloorGear() {
        return getEventItems(e -> e.getMinFloor() >= 40,
                item -> !"consumables".equals(item.getType()));
    }

    /**
     * 해당 층 범위의 이벤트(보상, 상점)에서 나오는 아이템 목록. 혼돈의 주사위 보상 후보로 쓴다.
     */
    public java.util.List<Item> getItemsForFloor(int fromFloor, int toFloor) {
        return getEventItems(e -> e.getMinFloor() <= toFloor && e.getMaxFloor() >= fromFloor,
                item -> true);
    }

    /**
     * 혼돈의 주사위 특별 선택지의 장비 후보: 지금보다 6~15층 깊은 이벤트의 보상 장비 (상점·소모품 제외).
     * 깊은 층에서는 41~50층 장비에서 고른다.
     */
    public java.util.List<Item> getUpperGear(int floor) {
        int from = Math.min(floor + 6, 41);
        int to = Math.min(floor + 15, 50);
        return getEventItems(e -> !(e instanceof com.textdungeon.event.ShopEvent)
                        && e.getMinFloor() <= to && e.getMaxFloor() >= from,
                item -> !"consumables".equals(item.getType()));
    }

    /** 지금 시나리오의 혼돈의 주사위 결과표 */
    public ChaosTable getChaosTable() {
        return chaosTable;
    }

    /**
     * 조건에 맞는 이벤트의 보상과 상점에서 나오는 아이템 목록.
     * 아이템 데이터에는 층 정보가 없으므로 getMonstersForFloor처럼 기존 이벤트 데이터로 판단한다.
     * 조건에 맞는 아이템이 하나도 없으면 전체 아이템으로 대체한다.
     */
    private java.util.List<Item> getEventItems(java.util.function.Predicate<GameEvent> eventFilter,
                                               java.util.function.Predicate<Item> itemFilter) {
        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
        for (GameEvent e : eventManager.getAll()) {
            if (!eventFilter.test(e)) continue;
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
            if (item != null && itemFilter.test(item)) result.add(item);
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