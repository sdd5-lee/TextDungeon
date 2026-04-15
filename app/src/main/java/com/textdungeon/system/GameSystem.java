package com.textdungeon.system;

import com.textdungeon.data.DataControl;
import com.textdungeon.model.Item;
import com.textdungeon.player.Player;
import com.textdungeon.event.GameEvent;

import java.util.List;
import java.util.Random;

public class GameSystem {

    private Player player;
    private List<GameEvent> events;
    private GameEvent currentEvent;

    private Random random = new Random();

    public GameSystem(Player player, List<GameEvent> events) {
        this.player = player;
        this.events = events;
    }

    // 탐험
    public String explore() {
        currentEvent = events.get(random.nextInt(events.size()));
        return currentEvent.getDescription();
    }

    // 선택 실행
    public String choose(int choice, DataControl<Item> itemManager) {
        return currentEvent.execute(player, choice, itemManager);
    }

    public List<String> getChoices() {
        return currentEvent.getChoices();
    }
    public GameEvent getRandomEvent() {
        //return new GameEvent();//게임이벤트에 들어갈 생성자 값 ...
        return null;
    }
}
