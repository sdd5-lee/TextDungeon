package com.textdungeon.system;

import android.content.Context;

import com.textdungeon.data.DataControlTower;
import com.textdungeon.data.Difficulty;
import com.textdungeon.model.Magic;
import com.textdungeon.model.Monster;
import com.textdungeon.model.Trait;
import com.textdungeon.player.Player;
import com.textdungeon.model.LearnedMagic;
import java.util.Random;

public class BattleSystem {

    private Player player;
    private Monster monster;
    private Context context;

    private String enemyName;
    private int enemyHp;
    private int enemyAttack;

    private int battleTurn;
    private boolean isPlayerTurn = true;
    private boolean isBattleOver = false;

    private Random random = new Random();

    public BattleSystem(Player player, Monster monster, Context context, Difficulty difficulty) {
        this.player = player;
        this.monster = monster;
        this.context = context;
        battleTurn = 1;

        this.enemyName = monster.getName();
        this.enemyHp = monster.getMaxHp() * difficulty.statMultiplier;
        this.enemyAttack = monster.getAttack() * difficulty.statMultiplier;
    }

    // =========================
    // 특성 검사 메서드
    // =========================
    private int applyTraitCritDamage(int damage) {
        Trait trait = player.getTrait();
        if (trait == null) return damage * 2;
        return trait.modifyCritDamage(player, damage * 2);
    }

    private int applyTraitIncomingDamage(int damage) {
        Trait trait = player.getTrait();
        if (trait == null) return damage;
        return trait.modifyIncomingDamage(player, damage);
    }

    private int applyTraitCounter(int damage) {
        Trait trait = player.getTrait();
        if (trait == null) return 0;
        return trait.modifyCounterDamage(player, damage);
    }

    private boolean isAlwaysEscape() {
        Trait trait = player.getTrait();
        return trait != null && trait.alwaysEscape();
    }

    private void applyTraitBattleEnd() {
        Trait trait = player.getTrait();
        if (trait != null) trait.onBattleEnd(player);
    }

    private boolean applyVampiricTouch() {
        Trait trait = player.getTrait();
        return trait != null && trait.bloodSucking();
    }
    private boolean applyMoreStrike() {
        Trait trait = player.getTrait();
        return trait != null && trait.triggerMoreStrike();
    }
    private boolean applyStruggle() {
        Trait trait = player.getTrait();
        return trait != null && trait.triggerStruggle();
    }

    // =========================
    // 플레이어 액션 (1: 공격, 2: 방어, 3: 도망, 4: 마법)
    // =========================
    public String playerAction(int choice, String magicId) {
        if (isBattleOver) return "전투가 이미 종료되었습니다.";
        if (!isPlayerTurn) return "적의 턴을 기다려주세요.";
        StringBuilder log = new StringBuilder();
        boolean isDefending = false;

        switch (choice) {
            case 1: // 일반 공격
                Trait trait = player.getTrait();
                int attackCount = 1;

                if (applyMoreStrike()) {
                    attackCount = trait.modifyStrikeCount();
                }

                for (int i = 0; i < attackCount; i++) {
                    if (enemyHp <= 0) {
                        i = attackCount;
                        break;
                    }
                    int damage = player.getFinalAtk() + random.nextInt(5);
                    if (random.nextInt(100) < player.getTotalCrit()) {
                        damage = applyTraitCritDamage(damage);
                        log.append("크리티컬! ");
                    }
                    int finalDamage = damage + ((damage/10) * battleTurn);
                    enemyHp -= finalDamage;
                    if (applyVampiricTouch()){
                        player.heal(finalDamage/10);
                    }
                    log.append("일반 공격! ").append(enemyName).append("에게 ").append(finalDamage).append("의 데미지!\n");
                }
                break;

            case 2: // 방어
                isDefending = true;
                log.append("방어 자세를 취합니다! (받는 데미지 절반)\n");
                break;

            case 3: // 도망
                if (isAlwaysEscape() || random.nextInt(100) < 40) {
                    isBattleOver = true;
                    return "성공적으로 도망쳤습니다!";
                } else {
                    log.append("도망에 실패했습니다!\n");
                }
                break;

            case 4: // 마법 사용
                if (magicId != null) {
                    LearnedMagic lm = player.getMagicScroll().getMagic(magicId);
                    if (lm == null || lm.getCurrentCount() <= 0) {
                        log.append("마법 사용 횟수가 부족합니다!\n");
                        return log.toString();
                    }
                    Magic magic = DataControlTower.getInstance(context).getMagicManager().spawn(lm.getMagicId());
                    int magicDamage = player.castMagic(magic);
                    int finalDamage = magicDamage + ((magicDamage/10) * battleTurn);
                    enemyHp -= finalDamage;
                    if (applyVampiricTouch()){
                        player.heal(finalDamage/10);
                    }
                    if (random.nextInt(100) < player.getTotalCrit()) {
                        finalDamage = applyTraitCritDamage(finalDamage);
                        log.append("크리티컬!   \n");
                    }
                    if (finalDamage > 0) {
                        enemyHp -= finalDamage;
                        log.append(magic.getName()).append(" 발동! ").append(enemyName).append("에게 ").append(finalDamage).append("의 데미지!\n");
                    }
                    break;
                }
        }

        // 몬스터 사망 체크
        if (enemyHp <= 0) {
            enemyHp = 0;
            isBattleOver = true;
            applyTraitBattleEnd();
            return log + enemyName + "을(를) 물리쳤습니다!";
        }

        if (applyStruggle()){
            battleTurn++;
        }
        isPlayerTurn = false;
        return log + "\n" + enemyTurn(isDefending);
    }

    // =========================
    // 적의 턴
    // =========================
    private String enemyTurn(boolean playerDefending) {
        int damage = enemyAttack + random.nextInt(3);
        String log = "";

        if (playerDefending) {
            damage /= 2;
            int counter = applyTraitCounter(damage);
            if (counter > 0) {
                enemyHp -= counter;
                log += "반격! " + enemyName + "에게 " + counter + "의 데미지!\n";
            }
        }

        damage = applyTraitIncomingDamage(damage);
        player.takeDamage(damage);
        log += enemyName + "의 공격! 플레이어에게 " + damage + "의 피해.";

        // 반격으로 몬스터 사망 체크
        if (enemyHp <= 0) {
            enemyHp = 0;
            isBattleOver = true;
            applyTraitBattleEnd();
            log += "\n반격으로 " + enemyName + "을(를) 물리쳤습니다!";
            isPlayerTurn = true;
            return log;
        }

        if (player.getStat().getHp() <= 0) {
            isBattleOver = true;
            log += "\n플레이어가 쓰러졌습니다...";
        }

        isPlayerTurn = true;
        return log;
    }

    // =========================
    // 게터
    // =========================
    public boolean isBattleOver() { return isBattleOver; }
    public String getEnemyName() { return enemyName; }
    public int getEnemyHp() { return enemyHp; }
}