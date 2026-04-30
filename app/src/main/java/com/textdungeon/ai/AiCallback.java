package com.textdungeon.ai;

import com.textdungeon.event.BattleEvent;

public interface AiCallback {
        void onSuccess(BattleEvent updatedEvent);
        void onError(String errorMessage);
}