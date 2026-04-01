package com.textdungeon.system;

import com.textdungeon.model.Monster;
import com.textdungeon.player.Player;

import java.util.Random;

public class BattleSystem {

    private Player player;
    private Monster monster;

    // 적 정보 (이벤트에서 가져옴)
    private String enemyName;
    private int enemyHp;
    private int enemyAttack;

    private boolean isPlayerTurn = true;
    private boolean isBattleOver = false;

    private Random random = new Random();

    public BattleSystem(Player player, Monster monster) {
        this.player = player;
        this.monster = monster;

        // 이벤트에서 적 정보 가져오기
        this.enemyName = monster.getName();
        this.enemyHp = monster.getHp();
        this.enemyAttack = monster.getAttack();
    }

    // =========================
    // 플레이어 턴
    // =========================
    public String playerAction(int choice) {

        if (isBattleOver) return "전투 종료됨";
        if (!isPlayerTurn) return "적의 턴입니다";

        String log = "";

        int playerDamage = player.getStat().getStrength() + random.nextInt(5);

        switch (choice) {

            case 1: // 공격
                enemyHp -= playerDamage;
                log += "공격! " + playerDamage + " 데미지\n";
                break;

            case 2: // 방어
                log += "방어 자세!\n";
                break;

            case 3: // 도망
                if (random.nextInt(100) < 50) {
                    isBattleOver = true;
                    return "도망 성공!";
                } else {
                    log += "도망 실패!\n";
                }
                break;
        }

        // 적 사망 체크
        if (enemyHp <= 0) {
            isBattleOver = true;
            return log + enemyName + " 처치!";
        }

        isPlayerTurn = false;
        return log + enemyTurn(choice == 2);
    }

    // =========================
    // 적 턴
    // =========================
    private String enemyTurn(boolean playerDefending) {

        int damage = enemyAttack + random.nextInt(5);

        if (playerDefending) {
            damage /= 2;
        }

        player.takeDamage(damage);

        String log = enemyName + " 공격! -" + damage + " HP";

        if (player.getStat().getHp() <= 0) {
            isBattleOver = true;
            log += "\n플레이어 사망...";
        }

        isPlayerTurn = true;
        return log;
    }

    // =========================
    // Getter
    // =========================
    public boolean isBattleOver() {
        return isBattleOver;
    }

    public String getEnemyName() {
        return enemyName;
    }

    public int getEnemyHp() {
        return enemyHp;
    }
}
