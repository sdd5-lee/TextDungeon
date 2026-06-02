package com.textdungeon.model;

import com.textdungeon.player.Player;
import java.util.Random;

public enum Trait {

    // 기존 직업 고유 특성
    INDOMITABLE("불굴", "HP가 30% 이하일 때 공격력이 50% 증가합니다.", 0) {
        @Override
        public int modifyAtk(Player player, int atk) {
            if (player.getStat().getHp() < player.getMaxHp() * 0.3) {
                return (int)(atk * 1.5);
            }
            return atk;
        }
    },
    MANA_SURGE("마력폭주", "마법 데미지가 50% 증가하지만 최대 HP가 20% 감소합니다.", 0) {
        @Override
        public int modifyMagicDamage(Player player, int damage) {
            return (int)(damage * 1.5);
        }
        @Override
        public int modifyMaxHp(Player player, int maxHp) {
            return (int)(maxHp * 0.8);
        }
    },
    SURVIVAL_INSTINCT("생존본능", "전투에서 도망이 항상 성공하며 크리티컬 확률이 10% 증가합니다.", 0) {
        @Override
        public int modifyCrit(Player player, int crit) {
            return crit + 10;
        }
        @Override
        public boolean alwaysEscape() {
            return true;
        }
    },
    SHARPSHOOTER("명사수", "크리티컬 발생 시 데미지가 2배가 됩니다.", 0) {
        @Override
        public int modifyCritDamage(Player player, int damage) {
            return damage * 2;
        }
    },
    IRON_SHIELD("견고한 방패", "받는 데미지가 20% 감소합니다.", 0) {
        @Override
        public int modifyIncomingDamage(Player player, int damage) {
            return (int)(damage * 0.8);
        }
    },
    NIRVANA("무아지경", "방어 시 받은 데미지의 30%를 반격합니다.\n 20% 확률로 두번 연속 공격합니다", 0) {
        @Override
        public int modifyCounterDamage(Player player, int incomingDamage) {
            return (int)(incomingDamage * 0.3);
        }
        @Override
        public boolean triggerMoreStrike() {
            return new Random().nextInt(100) > 80;
        }
        @Override
        public int modifyStrikeCount() {
            return 2;
        }
    },
    DIVINE_BLESSING("신성한 가호", "전투 승리 후 최대 HP의 20%를 회복합니다.", 0) {
        @Override
        public void onBattleEnd(Player player) {
            player.heal((int)(player.getMaxHp() * 0.2));
        }
    },
    DARK_PACT("어둠의 계약", "공격력과 마법 데미지가 모두 20% 증가합니다.", 0) {
        @Override
        public int modifyAtk(Player player, int atk) {
            return (int)(atk * 1.2);
        }
        @Override
        public int modifyMagicDamage(Player player, int damage) {
            return (int)(damage * 1.2);
        }
    },
    HEROES_WILL("용사의 의지", "HP가 50% 이하일 때 공격력, 크리티컬, 마법 데미지가 20% 증가합니다." , 0) {
        @Override
        public int modifyAtk(Player player, int atk) {
            if (player.getStat().getHp() < player.getMaxHp() * 0.5) return (int)(atk * 1.2);
            return atk;
        }
        @Override
        public int modifyCrit(Player player, int crit) {
            if (player.getStat().getHp() < player.getMaxHp() * 0.5) return (int)(crit * 1.2);
            return crit;
        }
        @Override
        public int modifyMagicDamage(Player player, int damage) {
            if (player.getStat().getHp() < player.getMaxHp() * 0.5) return (int)(damage * 1.2);
            return damage;
        }
    },

    // 범용 특성
    VAMPIRIC_TOUCH("흡혈의 손길", "물리 공격 및 마법 공격으로 가한 데미지의 10%만큼 체력을 회복합니다.", 3500){
        @Override
        public boolean bloodSucking() {
            return true;
        }
    },
    AWAKENED_BODY("각성된 신체", "기본 스탯(힘, 민첩, 체력, 지혜)이 영구적으로 5씩 증가합니다.", 3000) {
        @Override
        public boolean modifyBaseStat() {
            return true;
        }
        @Override
        public int modifyStatBonus() {
            return 5;
        }
    },
    DOUBLE_STRIKE("환검", "일반 공격 시 50% 확률로 3번 연속 공격합니다.", 4000) {
        @Override
        public boolean triggerMoreStrike() {
            return new Random().nextInt(100) < 50;
        }
        @Override
        public int modifyStrikeCount() {
            return 3;
        }
    },
    PERFECTIONIST("완벽주의자", "체력 100% 일때 데미지 5배", 3000) {
        @Override
        public int modifyAtk(Player player, int atk) {
            if (player.getStat().getHp() == player.getMaxHp()) return atk * 5;
            return atk;
        }
    },
    GLASS_CANNON("유리 대포", "가하는 모든 데미지가 2배가 되지만, 받는 데미지도 2배가 됩니다.", 2000) {
        @Override
        public int modifyAtk(Player player, int atk) { return atk * 2; }
        @Override
        public int modifyMagicDamage(Player player, int damage) { return damage * 2; }
        @Override
        public int modifyIncomingDamage(Player player, int damage) { return damage * 2; }
    },

    // 신들의 은총 특성
    BLESSING_OF_CHAOS("혼돈의 축복", "전투 중 크리티컬 확률이 50%로 고정됩니다.", 5000) {
        @Override
        public int modifyCrit(Player player, int crit) {
            return 50;
        }
    },
    BLESSING_OF_ADVENTURE("모험의 축복", "획득하는 경험치 양이 2배가 되고 모든 스텟이 5 증가합니다.", 20000) {
        @Override
        public boolean modifyBaseStat() {
            return true;
        }
        @Override
        public int modifyStatBonus() {
            return 5;
        }
        @Override
        public int modifyExp() {
            return 2;
        }
    },
    BLESSING_OF_DEATH("죽음의 축복", "HP가 30% 이하일 때, 가하는 모든 데미지가 3배가 됩니다.", 22000) {
        @Override
        public int modifyAtk(Player player, int atk) {
            if (player.getStat().getHp() <= player.getMaxHp() * 0.3) return atk * 3;
            return atk;
        }
        @Override
        public int modifyMagicDamage(Player player, int damage) {
            if (player.getStat().getHp() <= player.getMaxHp() * 0.3) return damage * 3;
            return damage;
        }
    },
    BLESSING_OF_STRUGGLE("투쟁의 축복", "턴이 지날때 마다 모든 데미지가 10% 증가합니다.", 20000) {
        @Override
        public boolean triggerStruggle() {
            return true;
        }
    },
    BLESSING_OF_TREASURE("보물의 축복", "모든 아이템중 랜덤으로 3개의 아이템을 획득합니다", 25000) {
        @Override
        public boolean triggerTreasure() {
            return true;
        }
    },
    DEMIGOD("반신 혈통", "혼돈의 신을 제외한 모든 신의 축복을 획득하고 모든스텟이 10증가한다", 100000) {
        @Override
        public boolean triggerTreasure() {
            return true;
        }
        @Override
        public boolean triggerStruggle() {
            return true;
        }
        @Override
        public int modifyAtk(Player player, int atk) {
            if (player.getStat().getHp() <= player.getMaxHp() * 0.3) return atk * 3;
            return atk;
        }
        @Override
        public int modifyMagicDamage(Player player, int damage) {
            if (player.getStat().getHp() <= player.getMaxHp() * 0.3) return damage * 3;
            return damage;
        }
        @Override
        public boolean modifyBaseStat() {
            return true;
        }
        @Override
        public int modifyStatBonus() {
            return 10;
        }
        @Override
        public int modifyExp() {
            return 2;
        }
    };

    public final String displayName;
    public final String description;
    public final int price;

    Trait(String displayName, String description, int price) {
        this.displayName = displayName;
        this.description = description;
        this.price = price;
    }

    public int modifyAtk(Player player, int atk)               { return atk; }
    public int modifyMagicDamage(Player player, int damage)    { return damage; }
    public int modifyMaxHp(Player player, int maxHp)           { return maxHp; }
    public int modifyCrit(Player player, int crit)             { return crit; }
    public int modifyCritDamage(Player player, int damage)     { return damage; }
    public int modifyIncomingDamage(Player player, int damage) { return damage; }
    public int modifyCounterDamage(Player player, int damage)  { return 0; }
    public int modifyExp()  { return 1; }
    public void onBattleEnd(Player player){ }
    public boolean alwaysEscape(){ return false; }
    public boolean triggerMoreStrike() { return false; }
    public int modifyStrikeCount() { return 0; }
    public boolean triggerTreasure() { return false; }
    public boolean modifyBaseStat() { return false; }
    public int modifyStatBonus() { return 0; }
    public boolean bloodSucking() { return false; }
    public boolean triggerStruggle() { return false; }
}