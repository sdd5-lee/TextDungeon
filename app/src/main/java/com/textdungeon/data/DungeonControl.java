package com.textdungeon.data;

public class DungeonControl {
    private int currentFloor;

    public DungeonControl(){
        currentFloor = 1;
    }
    public void nextCurrentFloor(){
        currentFloor++;
    }

    public void setCurrentFloor(int currentFloor) {
        this.currentFloor = currentFloor;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }
}
