package com.textdungeon.system;

import com.textdungeon.data.DataControlTower;
import com.textdungeon.model.Item;
import com.textdungeon.model.Stat;
import com.textdungeon.player.Player;

/**
 * 한 판이 끝났을 때(사망/클리어) 지급할 젬 계산.
 * DiedActivity와 ClearActivity에 똑같이 복사돼 있던 계산을 한 곳으로 모았다.
 */
public final class RunSettlement {
    private RunSettlement() {}

    public static int baseGems(DataControlTower dt, Player player) {
        Stat stat = player.getStat();

        // 스탯 총합 + 레벨 + 돈
        int gems = stat.getStrength() + stat.getWisdom() + stat.getHealth() + stat.getAgility()
                + stat.getGold() + player.getLevel();

        // 아이템 가치 합
        for (String id : player.getInventory().getItemMap().keySet()) {
            Item item = dt.getItemManager().spawn(id);
            if (item != null) gems += item.getValue();
        }

        // 배운 마법당 10
        gems += player.getMagicScroll().getLearnedMagics().size() * 10;

        // 이번 판에서 처치한 몬스터 수
        gems += player.getRunKillCount();

        return gems;
    }
}