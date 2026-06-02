package com.textdungeon.event;

public class BattleEvent extends GameEvent {
    protected String enemyId;
    public BattleEvent() {
        super();
    }
    public String getEnemyId() {
        return enemyId;
    }

}