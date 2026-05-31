package com.textdungeon.data;

import com.textdungeon.event.GameEvent;

public class DungeonControl {
    private int currentFloor;
    private GameEvent currentEvent;

    public DungeonControl(){
        currentFloor = 1;
    }

    public void nextCurrentFloor(){ currentFloor++; }
    public void setCurrentFloor(int currentFloor) { this.currentFloor = currentFloor; }
    public int getCurrentFloor() { return currentFloor; }

    public GameEvent getCurrentEvent() { return currentEvent; }
    public void setCurrentEvent(GameEvent event) { this.currentEvent = event; }
}