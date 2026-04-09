package com.textdungeon.data;

import android.content.Context;

import com.textdungeon.event.BattleEvent;
import com.textdungeon.model.Item;
import com.textdungeon.model.Monster;
import com.textdungeon.player.Job;
import com.textdungeon.model.Magic;
import com.textdungeon.player.Player;

public class DataControlTower {
    private static DataControlTower instance;
    public DataControl<Monster> monsterManager;
    public DataControl<Item> itemManager;
    public DataControl<BattleEvent> eventManager;
    public DataControl<Magic> magicManager;
    public Player player;

    private DataControlTower(Context context){
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

        eventManager = new DataControl<>(BattleEvent.class);
        eventManager.init(context, "event_list.json");

        magicManager = new DataControl<>(Magic.class);
        magicManager.init(context, "magic_list.json");
    }
    public void initPlayer(String name, Job job){
        if (player == null) {
            player = new Player(name,job);
        }
    }
}