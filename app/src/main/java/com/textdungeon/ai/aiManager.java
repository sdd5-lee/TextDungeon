package com.textdungeon.ai;

import com.textdungeon.event.GameEvent;
import com.textdungeon.model.Item;
import com.textdungeon.model.Stat;
import java.util.List;

public class aiManager {
    private final ChaosDice chaosDice;
    private final EventGenerator generator;

    public aiManager() {
        this.generator = new EventGenerator();
        this.chaosDice = new ChaosDice();
    }

    public void requestChaosChoice(int floor, Stat stat, List<Item> itemList, GameEvent currentEvent, AiCallback callback) {
        chaosDice.roll(floor, stat, itemList, currentEvent, callback);
    }

    public void generate(int targetFloor, Stat stat, List<Item> all, String normal, AiType randomGod, AiCallback aiCallback) {
        generator.generate(targetFloor,stat,all,normal,randomGod,aiCallback);
    }
}