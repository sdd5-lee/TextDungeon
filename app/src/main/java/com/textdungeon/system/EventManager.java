package com.textdungeon.system;

import android.util.Log;

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
    private int aiCount;

    public EventManager(DataControlTower dt) {
        this.dt = dt;
        this.player = dt.getPlayer();
        this.dungeonControl = dt.getDungeonControl();
        this.aiCount = dt.getDifficulty().eventCount;
    }

    // ─────────────────────────────────────────
    // 이벤트 선택
    // ─────────────────────────────────────────
    public GameEvent pickRandomEvent() {
        int currentFloor = dungeonControl.getCurrentFloor();
        DataControl<GameEvent> eventList = dt.getEventManager();

        if (dt.getAiEvents().containsKey(currentFloor)) {
            GameEvent aiEvent = dt.getAiEvents().get(currentFloor);
            dt.getAiEvents().remove(currentFloor);
            return aiEvent;
        }

        List<GameEvent> possibleEvents = eventList.getAll().stream()
                .filter(e -> currentFloor >= e.getMinFloor() && currentFloor <= e.getMaxFloor())
                .collect(Collectors.toList());

        if (possibleEvents.isEmpty()) {
            // 이 층을 담당하는 이벤트가 없으면(데이터 수정 실수 등) 크래시 대신 전체 이벤트에서 고른다
            Log.w("EventManager", currentFloor + "F에 해당하는 이벤트가 없어 전체 이벤트에서 선택합니다.");
            possibleEvents = eventList.getAll();
            if (possibleEvents.isEmpty()) {
                throw new IllegalStateException("이벤트 데이터가 비어 있습니다.");
            }
        }

        return possibleEvents.get(new Random().nextInt(possibleEvents.size()));
    }
    // ─────────────────────────────────────────
    // 이벤트 결과 처리
    // ─────────────────────────────────────────
    public String applyReward(GameEvent event, int choiceIndex) {
        return applyReward(event, choiceIndex, false);
    }

    /**
     * @param giveUpItemIfFull true면 가방이 가득 차도 아이템만 포기하고 나머지 보상을 적용한다
     * @return 결과 문구, 또는 가방이 가득 차서 확인이 필요하면 "full"
     */
    public String applyReward(GameEvent event, int choiceIndex, boolean giveUpItemIfFull) {
        boolean noRoom = event.hasItemReward(choiceIndex)
                && !player.getInventory().canAdd(event.getItemId(choiceIndex));
        if (noRoom && !giveUpItemIfFull) {
            return "full";
        }
        String result = event.execute(player, choiceIndex, dt.getItemManager(),dt.getDifficulty());

        if (player.getInventory() != null && player.getInventory().getItemMap() != null) {
            for (String itemId : player.getInventory().getItemMap().keySet()) {
                dt.getUserRecord().getDiscoveredItems().add(itemId);
            }
        }
        dt.saveGame();
        if (noRoom) {
            result += "\n(가방이 가득 차서 아이템은 챙기지 못했습니다)";
        }
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
        // 끝난 이벤트를 지워야 화면이 다시 만들어질 때(회전 등) 같은 이벤트가 다시 떠서 보상을 또 받는 일이 없다
        dungeonControl.setCurrentEvent(null);
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
    // 게임오버 판단
    // ─────────────────────────────────────────
    public boolean isPlayerDead() {
        return player.getStat().getHp() <= 0;
    }

}