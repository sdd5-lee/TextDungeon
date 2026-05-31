package com.textdungeon.data;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.textdungeon.ai.aiManager;
import com.textdungeon.event.BattleEvent;
import com.textdungeon.event.GameEvent;
import com.textdungeon.event.ShopEvent;
import com.textdungeon.model.Item;
import com.textdungeon.model.Monster;
import com.textdungeon.model.Job;
import com.textdungeon.model.Magic;
import com.textdungeon.player.Player;
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
    private aiManager aiManager;
    private Difficulty difficulty;
    private Map<Integer, GameEvent> aiEvents;
    private DataControlTower(Context context){
        this.appContext = context.getApplicationContext();
        this.aiManager = new aiManager();
        this.aiEvents = new HashMap<>();
        initAll(context);
        loadGameData();
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

        RuntimeTypeAdapterFactory<GameEvent> eventAdapterFactory =
                RuntimeTypeAdapterFactory.of(GameEvent.class, "type")
                        .registerSubtype(BattleEvent.class, "battle")
                        .registerSubtype(GameEvent.class, "normal")
                        .registerSubtype(ShopEvent.class, "shop");

        Gson eventGson = new GsonBuilder()
                .registerTypeAdapterFactory(eventAdapterFactory)
                .create();
        eventManager = new DataControl<>(GameEvent.class, eventGson);
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
        this.dungeonControl = new DungeonControl();
        if (save != null){
            this.player = save.getPlayer();
            this.difficulty = save.getDifficulty();
            this.dungeonControl.setCurrentFloor(save.getCurrentFloor());
        }else {
            this.player = null;
            this.difficulty = Difficulty.NORMAL;
            this.dungeonControl.setCurrentFloor(1);
        }
    }
    public void startNewGame(String name, Job job, String traitId){
        this.player = GameSave.createNewPlayer(this.userRecord, name,job,traitId);

        this.player.setTraitId(traitId);

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
        if (this.player == null){return;}
        GameSave currentSave = new GameSave(this.player, dungeonControl.getCurrentFloor(),difficulty,aiEvents);
        currentSave.runSave(appContext);
    }
    public void resetRun() {
        this.player = null;
        GameSave.deleteRun(appContext);
    }

    public Context getAppContext() {
        return appContext;
    }

    public DataControl<GameEvent> getEventManager() {
        return eventManager;
    }

    public DataControl<Item> getItemManager() {
        return itemManager;
    }

    public DataControl<Magic> getMagicManager() {
        return magicManager;
    }

    public DataControl<Monster> getMonsterManager() {
        return monsterManager;
    }

    public Player getPlayer() {
        return player;
    }
    public UserRecord getUserRecord() {
        return userRecord;
    }

    public DungeonControl getDungeonControl() {
        return dungeonControl;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public aiManager getAiManager() {
        return aiManager;
    }
    public void setUserRecord(UserRecord userRecord) {
        this.userRecord = userRecord;
    }

    public void setDifficulty(String difficultyName) {
        difficulty = Difficulty.valueOf(difficultyName);
    }
    public Difficulty getDifficulty() {
        return difficulty;
    }

    public Map<Integer,GameEvent> getAiEvents() {
        return aiEvents;
    }
    public void addAiEvent(int targetFloor, GameEvent newEvent) {
        aiEvents.put(targetFloor, newEvent);
    }
    private void giveRandomStartingItem(Player p) {
        Item randomItem = itemManager.getRandomData();

        if (randomItem != null) {
            p.getInventory().addItem(randomItem);
        }
    }
}