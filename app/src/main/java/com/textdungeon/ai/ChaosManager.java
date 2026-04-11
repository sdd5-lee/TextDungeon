package com.textdungeon.ai;

import com.textdungeon.event.BattleEvent;

public class ChaosManager {
    private ChaosDice chaosDice;
    public ChaosManager(){
        chaosDice = new ChaosDice();
    }


    public void requestChaosChoice(BattleEvent currentEvent, AiCallback callback) {
        chaosDice.roll(currentEvent, callback);
    }
}
