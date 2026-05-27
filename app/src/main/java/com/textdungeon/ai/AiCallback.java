package com.textdungeon.ai;

import com.textdungeon.event.GameEvent;

public interface AiCallback {
        void onSuccess(GameEvent updatedEvent);
        void onError(String errorMessage);
}