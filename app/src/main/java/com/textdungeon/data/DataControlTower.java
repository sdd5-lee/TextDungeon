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

public class DataControlTower {
    private static DataControlTower instance;
    private Context appContext; // 저장을 위해 앱 컨텍스트를 기억해 둡니다.
    private DataControl<Monster> monsterManager;
    private DataControl<Item> itemManager;
    private DataControl<BattleEvent> eventManager;
    private DataControl<Magic> magicManager;
    private Player player;
    private UserRecord userRecord;
    private DataControlTower(Context context){
        this.appContext = context.getApplicationContext();
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
    }
    private void loadGameData() {
        GameSave save = GameSave.load(appContext);
        if (save != null) {
            this.player = save.getPlayer();

            if (save.getUserRecord() != null) {
                this.userRecord = save.getUserRecord();
            } else {
                this.userRecord = new UserRecord();
            }
        } else {
            this.userRecord = new UserRecord();
        }
    }
    public void saveGame() {
        GameSave currentSave = new GameSave(this.player, this.userRecord);
        currentSave.save(appContext);
    }
    public void initPlayer(String name, Job job){
        if (player == null) {
            player = new Player(name,job);
        }
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

    public UserRecord getUserRecord() {
        return userRecord;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void setUserRecord(UserRecord userRecord) {
        this.userRecord = userRecord;
    }
}