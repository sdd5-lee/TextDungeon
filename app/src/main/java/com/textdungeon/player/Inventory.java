package com.textdungeon.player;

import com.textdungeon.model.Item;

import java.util.HashMap;
import java.util.Map;

public class Inventory {
    private Map<String,Integer> itemMap;
    private static final int MAX_INV = 30;
    public Inventory(){
        itemMap = new HashMap<>();
    }
    public Map<String, Integer> getItemMap() {
        return itemMap;
    }

    public void addItem(Item item) {
        if(item == null) {return;}
        String itemId = item.getId();
        if (itemMap.containsKey(itemId)){
            itemMap.put(itemId, itemMap.get(itemId)+1);
        }
        else if (itemMap.size() < MAX_INV) {
            itemMap.put(itemId,1);
        }
    }
    public boolean isItem(String itemId) {
        return getItemMap().containsKey(itemId);
    }
    public boolean consumeItem(String itemId) {
        if (itemMap.containsKey(itemId)) {
            int count = itemMap.get(itemId);
            if (count > 1) {
                itemMap.put(itemId, count - 1);
            } else {
                itemMap.remove(itemId);
            }
            return true;
        }
        return false;
    }
    public void removeItem(String itemId) {
        if (itemMap.containsKey(itemId)) {
            itemMap.remove(itemId);
        }
    }
    /** 이 아이템을 넣을 자리가 있는지. 이미 가진 아이템은 같은 칸에 쌓이므로 가방이 꽉 차도 넣을 수 있다. */
    public boolean canAdd(String itemId) {
        return itemMap.containsKey(itemId) || itemMap.size() < MAX_INV;
    }
    public int getCount(String itemId) {
        Integer c = itemMap.get(itemId);
        return c == null ? 0 : c;
    }
    public boolean isFullItem() {
        return itemMap.size() >= MAX_INV;
    }
}