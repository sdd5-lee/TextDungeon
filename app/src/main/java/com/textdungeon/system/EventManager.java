package com.textdungeon.system;

import com.textdungeon.data.DataControl;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.data.DungeonControl;
import com.textdungeon.event.BattleEvent;
import com.textdungeon.event.GameEvent;
import com.textdungeon.model.Monster;
import com.textdungeon.player.Player;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 이벤트 화면의 게임 로직을 담당하는 클래스.
 * EventLayout(UI)과 분리되어 순수 게임 로직만 처리한다.
 *
 * 담당:
 *  - 현재 층에 맞는 랜덤 이벤트 선택
 *  - 이벤트 결과(보상) 처리
 *  - 레벨업 여부 판단
 *  - 층 이동
 *  - 전투용 몬스터 소환
 */
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

    /**
     * 현재 층에 맞는 이벤트 목록에서 랜덤으로 하나 반환.
     * @throws IllegalStateException 해당 층에 이벤트가 없을 때
     */
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

    /**
     * 선택지에 따른 보상을 플레이어에 적용하고 결과 문자열을 반환.
     * TODO: 인벤토리 UI 완성 후 아래 주석을 해제할 것.
     */
    public String applyReward(GameEvent event, int choiceIndex) {
        // TODO: 인벤토리 버리기 UI 완성 후 아래 블록 주석 해제
        // if (player.getInventory().isFullItem()) {
        //     return null;
        // }
        String result = event.execute(player, choiceIndex, dt.getItemManager());
        dt.saveGame();
        return result;
    }

    // ─────────────────────────────────────────
    // 레벨업 판단
    // ─────────────────────────────────────────

    /**
     * 보상 적용 전 레벨을 저장해 두고, 적용 후 레벨과 비교해 레벨업 여부를 반환.
     */
    public boolean didLevelUp(int levelBeforeReward) {
        return levelBeforeReward < player.getLevel();
    }

    /** 보상 적용 직전에 호출해 현재 레벨을 스냅샷으로 반환한다. */
    public int snapshotLevel() {
        return player.getLevel();
    }

    // ─────────────────────────────────────────
    // 층 이동
    // ─────────────────────────────────────────

    /** 다음 층으로 이동하고 저장한다. */
    public void goNextFloor() {
        dungeonControl.nextCurrentFloor();
        dt.saveGame();
    }

    /** 현재 층 번호를 반환한다. */
    public int getCurrentFloor() {
        return dungeonControl.getCurrentFloor();
    }

    // ─────────────────────────────────────────
    // 전투용 몬스터 소환
    // ─────────────────────────────────────────

    /**
     * 이벤트에 연결된 몬스터를 소환한다.
     * 몬스터 데이터가 없으면 null 반환.
     */
    public Monster spawnMonster(String monsterId) {
        if (monsterId == null || monsterId.isEmpty()) return null;
        return dt.getMonsterManager().spawn(monsterId);
    }

    // ─────────────────────────────────────────
    // 게임오버 판단 (2번 구현 시 여기에 추가)
    // ─────────────────────────────────────────

    /** 플레이어가 사망 상태인지 확인한다. */
    public boolean isPlayerDead() {
        return player.getStat().getHp() <= 0;
    }
}