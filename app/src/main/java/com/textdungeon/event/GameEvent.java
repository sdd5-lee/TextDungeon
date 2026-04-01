package com.textdungeon.event;

import com.textdungeon.player.Player;

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
    void multipleChoice(boolean choice){

    }
    void reWord(Player player){

    }
}
