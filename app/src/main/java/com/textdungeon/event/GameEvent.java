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

    public String getEnemyName() {
        return "";
    }

    public String[] getChoices() {
        return null;
    }

    public int getEnemyAttack() {
        return 0;
    }

    public int getEnemyHp() {
        return 0;
    }

    public String execute(Player player, int choice) {
        return "";
    }

    public String getDescription() {
        return "";
    }
}
