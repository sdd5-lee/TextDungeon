package com.textdungeon.event;

import com.google.gson.Gson;
import com.textdungeon.data.DataControl;
import com.textdungeon.model.Monster;
import com.textdungeon.player.Player;
import com.textdungeon.system.BattleSystem;
public class BattleEvent extends GameEvent {
    public BattleEvent() {
        super();
    }
    public static BattleEvent createFromJson(String json) {
        return new Gson().fromJson(json, BattleEvent.class);
    }

    public BattleSystem startBattle(Player player,DataControl<Monster> monsterManager) {
        Monster enemy = spawnEnemy(monsterManager);
        if (enemy == null) return null;

        return new BattleSystem(player, enemy);
    }

    public Monster spawnEnemy(DataControl<Monster> monsterManager) {
        if (enemyId == null || enemyId.isEmpty()) return null;
        return monsterManager.spawn(enemyId);
    }

}