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

    public void removeItem(String itemId) {
        consumeItem(itemId);
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
}
