package com.textdungeon.data;

import android.content.Context;

import com.textdungeon.event.BattleEvent;
import com.textdungeon.model.Item;
import com.textdungeon.model.Monster;
import com.textdungeon.model.Job;
import com.textdungeon.model.Magic;
import com.textdungeon.player.Player;
import com.textdungeon.system.GameSave;
import com.textdungeon.system.UserRecord;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class DataControlTower {
    private static DataControlTower instance;
    private final Context appContext;
    private DataControl<Monster> monsterManager;
    private DataControl<Item> itemManager;
    private DataControl<BattleEvent> eventManager;
    private DataControl<Magic> magicManager;
    private Player player;
    private UserRecord userRecord;
    private DungeonControl dungeonControl;
    private final OkHttpClient httpClient;
    private DataControlTower(Context context){
        this.appContext = context.getApplicationContext();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
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

        eventManager = new DataControl<>(BattleEvent.class);
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
            this.dungeonControl.setCurrentFloor(save.getCurrentFloor());
        }else {
            this.player = null;
            this.dungeonControl.setCurrentFloor(1);
        }
    }
    public void startNewGame(String name, Job job){
        this.player = GameSave.createNewPlayer(this.userRecord, name,job);
        this.dungeonControl.setCurrentFloor(1);
        saveGame();
    }

    public void saveGame() {
        if (this.player == null){return;}
        GameSave currentSave = new GameSave(this.player, dungeonControl.getCurrentFloor());
        currentSave.runSave(appContext);
    }
    public void resetRun() {
        this.player = null;
        GameSave.deleteRun(appContext);
    }

    public Context getAppContext() {
        return appContext;
    }

    public DataControl<BattleEvent> getEventManager() {
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
    public OkHttpClient getHttpClient() {
        return httpClient;
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

    public void setUserRecord(UserRecord userRecord) {
        this.userRecord = userRecord;
    }
}