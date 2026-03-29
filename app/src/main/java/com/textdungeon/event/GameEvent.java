package com.textdungeon.event;

public class GameEvent {
    int stage;
    int id;
    String name;
    String description;
    int floor;
    public GameEvent(int id, String name, String description, int floor){
        this.id = id;
        this.name = name;
        this.description = description;
        this.floor = floor;
    }
    void nextStage(){

    }
    void viewText(){

    }
    void multipleChoice(boolean choice){

    }
}
