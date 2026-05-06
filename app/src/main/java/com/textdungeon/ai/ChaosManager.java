package com.textdungeon.ai;

import com.textdungeon.event.BattleEvent;
import com.textdungeon.model.Item;
import com.textdungeon.model.Stat;
import java.util.List;

public class ChaosManager {
    private final ChaosDice chaosDice;

    public ChaosManager() {
        this.chaosDice = new ChaosDice();
    }

    public void requestChaosChoice(int floor, Stat stat, List<Item> itemList, BattleEvent currentEvent, AiCallback callback) {
        chaosDice.roll(floor, stat, itemList, currentEvent, callback);
    }
}