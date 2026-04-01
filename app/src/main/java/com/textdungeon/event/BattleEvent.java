package com.textdungeon.event;

import com.google.gson.Gson;
import com.textdungeon.model.Monster;
import com.textdungeon.model.MonsterList;
import com.textdungeon.player.Player;
import com.textdungeon.system.BattleSystem;
public class BattleEvent extends GameEvent {
    private String enemyId;
    public BattleEvent() {
        super();
    }
    public static BattleEvent createFromJson(String json) {
        return new Gson().fromJson(json, BattleEvent.class);
    }

    public BattleSystem startBattle(Player player) {
        Monster enemy = spawnEnemy();
        if (enemy == null) return null;

        return new BattleSystem(player, enemy);
    }


    public Monster spawnEnemy() {
        if (enemyId == null || enemyId.isEmpty()) return null;
        return MonsterList.spawn(enemyId);
    }

    public String getEnemyId() { return enemyId; }

    public String getEnemyName() {
        Monster m = spawnEnemy();
        return (m != null) ? m.getName() : "강력한 적";
    }
}