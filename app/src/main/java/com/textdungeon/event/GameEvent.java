package com.textdungeon.event;

import com.google.gson.Gson;

public class GameEvent {
    protected String id;
    protected String name;
    protected String description;
    protected int floor;

    public GameEvent() {}
    public static GameEvent createFromJson(String json) {
        return new Gson().fromJson(json, GameEvent.class);
    }

    // 기본 Getter들
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getFloor() { return floor; }
}