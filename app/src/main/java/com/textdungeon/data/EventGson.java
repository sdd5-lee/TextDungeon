package com.textdungeon.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.textdungeon.event.BattleEvent;
import com.textdungeon.event.GameEvent;
import com.textdungeon.event.ShopEvent;

/**
 * GameEvent의 하위 클래스(BattleEvent/ShopEvent)를 구분해서 읽고 쓰는 Gson.
 * 이벤트 데이터 로드, AI 이벤트 파싱, 세이브 파일 저장/불러오기에서 같은 설정을 쓰기 위해 한 곳에 모았다.
 * (일반 Gson으로 저장하면 BattleEvent의 enemyId, ShopEvent의 shopItems가 불러올 때 사라진다)
 */
public final class EventGson {
    private EventGson() {}

    public static Gson create() {
        RuntimeTypeAdapterFactory<GameEvent> factory =
                // maintainType = true: 'type' 값을 객체의 type 필드에 그대로 둔다.
                // false면 읽을 때 type 필드가 null이 되고, 저장할 때 그 null 필드 때문에
                // "already defines a field named type" 예외로 저장 자체가 실패한다.
                RuntimeTypeAdapterFactory.of(GameEvent.class, "type", true)
                        .registerSubtype(BattleEvent.class, "battle")
                        .registerSubtype(GameEvent.class, "normal")
                        .registerSubtype(ShopEvent.class, "shop");
        return new GsonBuilder()
                .registerTypeAdapterFactory(factory)
                .create();
    }
}