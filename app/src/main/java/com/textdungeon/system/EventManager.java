package com.textdungeon.system;

import com.textdungeon.data.DataControl;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.data.DungeonControl;
import com.textdungeon.event.GameEvent;
import com.textdungeon.model.Monster;
import com.textdungeon.player.Player;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
public class EventManager {

    private final DataControlTower dt;
    private final Player player;
    private final DungeonControl dungeonControl;

    public EventManager(DataControlTower dt) {
        this.dt = dt;
        this.player = dt.getPlayer();
        this.dungeonControl = dt.getDungeonControl();
    }

    // ─────────────────────────────────────────
    // 이벤트 선택
    // ─────────────────────────────────────────
    public GameEvent pickRandomEvent() {
        int currentFloor = dungeonControl.getCurrentFloor();
        DataControl<GameEvent> eventList = dt.getEventManager();

        List<GameEvent> possibleEvents = eventList.getAll().stream()
                .filter(e -> currentFloor >= e.getMinFloor() && currentFloor <= e.getMaxFloor())
                .collect(Collectors.toList());

        if (possibleEvents.isEmpty()) {
            throw new IllegalStateException(currentFloor + "F에 해당하는 이벤트가 없습니다.");
        }

        return possibleEvents.get(new Random().nextInt(possibleEvents.size()));
    }

    // ─────────────────────────────────────────
    // 이벤트 결과 처리
    // ─────────────────────────────────────────
    public String applyReward(GameEvent event, int choiceIndex) {
         if (player.getInventory().isFullItem()) {
             return null;
         }
        String result = event.execute(player, choiceIndex, dt.getItemManager(),dt.getDifficulty());
        dt.saveGame();
        return result;
    }

    // ─────────────────────────────────────────
    // 레벨업 판단
    // ─────────────────────────────────────────S
    public boolean didLevelUp(int levelBeforeReward) {
        return levelBeforeReward < player.getLevel();
    }
    public int snapshotLevel() {
        return player.getLevel();
    }

    // ─────────────────────────────────────────
    // 층 이동
    // ─────────────────────────────────────────
    public void goNextFloor() {
        dungeonControl.nextCurrentFloor();
        dt.saveGame();
    }
    public int getCurrentFloor() {
        return dungeonControl.getCurrentFloor();
    }

    // ─────────────────────────────────────────
    // 전투용 몬스터 소환
    // ─────────────────────────────────────────
    public Monster spawnMonster(String monsterId) {
        if (monsterId == null || monsterId.isEmpty()) return null;
        return dt.getMonsterManager().spawn(monsterId);
    }

    // ─────────────────────────────────────────
    // 게임오버 판단 (2번 구현 시 여기에 추가)
    // ─────────────────────────────────────────
    public boolean isPlayerDead() {
        return player.getStat().getHp() <= 0;
    }
}