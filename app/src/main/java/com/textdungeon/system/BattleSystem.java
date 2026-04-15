package com.textdungeon.system;

import android.content.Context;
import com.textdungeon.model.Monster;
import com.textdungeon.player.Player;
import com.textdungeon.model.LearnedMagic;
import java.util.Random;
import java.util.List;

public class BattleSystem {

    private Player player;
    private Monster monster;
    private Context context; // 마법 데이터 조회를 위해 필요

    private String enemyName;
    private int enemyHp;
    private int enemyAttack;

    private boolean isPlayerTurn = true;
    private boolean isBattleOver = false;

    private Random random = new Random();

    public BattleSystem(Player player, Monster monster, Context context) {
        this.player = player;
        this.monster = monster;
        this.context = context;

        // BattleEvent에서 넘어온 몬스터 정보 초기화
        this.enemyName = monster.getName();
        this.enemyHp = monster.getHp();
        this.enemyAttack = monster.getAttack();
    }

    // =========================
    // 플레이어 액션 (1: 공격, 2: 방어, 3: 도망, 4: 마법)
    // =========================
    public String playerAction(int choice, String magicId) {
        if (isBattleOver) return "전투가 이미 종료되었습니다.";
        if (!isPlayerTurn) return "적의 턴을 기다려주세요.";

        String log = "";
        boolean isDefending = false;

        switch (choice) {
            case 1: // 일반 공격
                int damage = player.getFinalAtk() + random.nextInt(5);
                enemyHp -= damage;
                log += "⚔️ 일반 공격! " + enemyName + "에게 " + damage + "의 데미지!\n";
                break;

            case 2: // 방어
                isDefending = true;
                log += "🛡️ 방어 자세를 취합니다! (받는 데미지 절반)\n";
                break;

            case 3: // 도망
                if (random.nextInt(100) < 40) { // 40% 확률 도망
                    isBattleOver = true;
                    return "💨 성공적으로 도망쳤습니다!";
                } else {
                    log += "🏃 도망에 실패했습니다!\n";
                }
                break;

            case 4: // 마법 사용
                if (magicId != null) {
                    int magicDamage = player.castMagic(magicId, context);
                    if (magicDamage > 0) {
                        enemyHp -= magicDamage;
                        log += "🔥 마법 발동! " + enemyName + "에게 " + magicDamage + "의 광역 데미지!\n";
                    } else {
                        log += "❌ 마법 사용 횟수가 부족하거나 마법을 찾을 수 없습니다!\n";
                        return log; // 턴을 소비하지 않음
                    }
                }
                break;
        }

        // 몬스터 사망 체크
        if (enemyHp <= 0) {
            enemyHp = 0;
            isBattleOver = true;
            return log + "🎉 " + enemyName + "을(를) 물리쳤습니다!";
        }

        isPlayerTurn = false;
        return log + "\n" + enemyTurn(isDefending);
    }

    // =========================
    // 적의 턴
    // =========================
    private String enemyTurn(boolean playerDefending) {
        int damage = enemyAttack + random.nextInt(3);

        if (playerDefending) {
            damage /= 2; // 방어 시 데미지 감소
        }

        player.takeDamage(damage);

        String log = "👾 " + enemyName + "의 공격! 플레이어에게 " + damage + "의 피해.";

        if (player.getStat().getHp() <= 0) {
            isBattleOver = true;
            log += "\n💀 플레이어가 쓰러졌습니다...";
        }

        isPlayerTurn = true;
        return log;
    }

    // =========================
    // 게터 및 보조 메서드
    // =========================
    public boolean isBattleOver() { return isBattleOver; }
    public String getEnemyName() { return enemyName; }
    public int getEnemyHp() { return enemyHp; }
    
    // 현재 사용 가능한 마법 리스트 반환 (UI 연결용)
    public List<LearnedMagic> getAvailableMagics() {
        return player.getMagicScroll().getLearnedMagics();
    }
}
