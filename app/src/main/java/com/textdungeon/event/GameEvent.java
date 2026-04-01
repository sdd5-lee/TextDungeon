package com.textdungeon.event;

import com.google.gson.Gson;

public class GameEvent {
    protected int id;
    protected String name;
    protected String description;
    protected int floor;

    public GameEvent() {}

    // 기본 Getter들
    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getFloor() { return floor; }
}