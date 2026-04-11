package com.textdungeon.data;

import android.util.Log;

import com.textdungeon.event.GameEvent;
import com.textdungeon.model.Item;
import com.textdungeon.model.Monster;
import com.textdungeon.model.Reward;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DataValidator {
    private static final String TAG = "DataValidator";

    public static boolean validateAll(
            List<Monster> monsters,
            List<Item> items,
            List<? extends GameEvent> events   // 🔥 여기 수정
    ) {
        boolean isValid = true;

        Set<String> monsterIds = new HashSet<>();
        Set<String> itemIds = new HashSet<>();
        Set<String> eventIds = new HashSet<>();

        isValid &= validateMonsterIds(monsters, monsterIds);
        isValid &= validateItemIds(items, itemIds);
        isValid &= validateEventIds(events, eventIds);
        isValid &= validateEventReferences(events, monsterIds, itemIds);

        if (isValid) {
            Log.d(TAG, "모든 데이터 무결성 검사 통과");
        } else {
            Log.e(TAG, "데이터 무결성 검사 실패");
        }

        return isValid;
    }

    private static boolean validateMonsterIds(List<Monster> monsters, Set<String> monsterIds) {
        boolean isValid = true;

        for (Monster monster : monsters) {
            if (monster == null) {
                Log.e(TAG, "monster 데이터에 null이 있습니다.");
                isValid = false;
                continue;
            }

            String id = monster.getId();

            if (id == null || id.trim().isEmpty()) {
                Log.e(TAG, "monster id가 비어 있습니다. name=" + monster.getName());
                isValid = false;
                continue;
            }

            if (!monsterIds.add(id)) {
                Log.e(TAG, "중복 monster id 발견: " + id);
                isValid = false;
            }
        }

        return isValid;
    }

    private static boolean validateItemIds(List<Item> items, Set<String> itemIds) {
        boolean isValid = true;

        for (Item item : items) {
            if (item == null) {
                Log.e(TAG, "item 데이터에 null이 있습니다.");
                isValid = false;
                continue;
            }

            String id = item.getId();

            if (id == null || id.trim().isEmpty()) {
                Log.e(TAG, "item id가 비어 있습니다. name=" + item.getName());
                isValid = false;
                continue;
            }

            if (!itemIds.add(id)) {
                Log.e(TAG, "중복 item id 발견: " + id);
                isValid = false;
            }
        }

        return isValid;
    }

    private static boolean validateEventIds(List<? extends GameEvent> events, Set<String> eventIds) {
        boolean isValid = true;

        for (GameEvent event : events) {
            if (event == null) {
                Log.e(TAG, "event 데이터에 null이 있습니다.");
                isValid = false;
                continue;
            }

            String id = event.getId();

            if (id == null || id.trim().isEmpty()) {
                Log.e(TAG, "event id가 비어 있습니다. name=" + event.getName());
                isValid = false;
                continue;
            }

            if (!eventIds.add(id)) {
                Log.e(TAG, "중복 event id 발견: " + id);
                isValid = false;
            }
        }

        return isValid;
    }

    private static boolean validateEventReferences(
            List<? extends GameEvent> events,   // 🔥 여기 수정
            Set<String> monsterIds,
            Set<String> itemIds
    ) {
        boolean isValid = true;

        for (GameEvent event : events) {
            if (event == null) continue;

            List<String> choices = event.getChoices();
            List<Reward> rewards = event.getRewards();

            if (choices == null) {
                Log.e(TAG, "choices가 null입니다. eventId=" + event.getId());
                isValid = false;
            }

            if (rewards == null) {
                Log.e(TAG, "rewards가 null입니다. eventId=" + event.getId());
                isValid = false;
            }

            if (choices != null && rewards != null && choices.size() != rewards.size()) {
                Log.e(TAG, "choices와 rewards 개수가 다릅니다. eventId=" + event.getId()
                        + ", choices=" + choices.size()
                        + ", rewards=" + rewards.size());
                isValid = false;
            }

            String enemyId = event.getEnemyId();
            if (enemyId != null && !enemyId.trim().isEmpty() && !monsterIds.contains(enemyId)) {
                Log.e(TAG, "존재하지 않는 enemyId 참조. eventId=" + event.getId()
                        + ", enemyId=" + enemyId);
                isValid = false;
            }

            if (rewards != null) {
                for (int i = 0; i < rewards.size(); i++) {
                    Reward reward = rewards.get(i);

                    if (reward == null) {
                        Log.e(TAG, "reward가 null입니다. eventId=" + event.getId() + ", index=" + i);
                        isValid = false;
                        continue;
                    }

                    String itemId = reward.getItemId();
                    if (itemId != null && !itemId.trim().isEmpty() && !itemIds.contains(itemId)) {
                        Log.e(TAG, "존재하지 않는 itemId 참조. eventId=" + event.getId()
                                + ", rewardIndex=" + i
                                + ", itemId=" + itemId);
                        isValid = false;
                    }
                }
            }
        }

        return isValid;
    }
}