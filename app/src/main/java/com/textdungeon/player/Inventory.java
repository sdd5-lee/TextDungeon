package com.textdungeon.player;

import com.textdungeon.model.Item;

import java.util.HashMap;
import java.util.Map;

public class Inventory {
    private Map<String,Integer> itemMap;
    private static final int MAX_INV = 30;
    public Map<String, Integer> getItemMap() {
        return itemMap;
    }

    public void removeItem(String itemName) {
        if (itemMap.containsKey(itemName)) {
            int count = itemMap.get(itemName);
            if (count > 1) itemMap.put(itemName, count - 1);
            else itemMap.remove(itemName);
        }
    }
    public void addItem(Item item) {
        if(item == null) {return;}
        String itemName = item.getName();
        if (itemMap.containsKey(itemName)){
            itemMap.put(itemName, itemMap.get(itemName)+1);
        }
        else if (itemMap.size() < MAX_INV) {
            itemMap.put(itemName,1);
        }
    }
}
