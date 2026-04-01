package com.textdungeon.model;

import android.content.Context;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class ItemList {
    private static Map<String, String> itemList = new HashMap<>();

    public static Item getItem(String id) {
        String json = itemList.get(id);
        return Item.createFromJson(json);
    }

    public static void init(Context context) {
        String fileName = "item_list.json";

        try (InputStream is = context.getAssets().open(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            parseitemList(sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void parseitemList(String json) {
        Gson gson = new Gson();

        Monster[] monsterArray = gson.fromJson(json, Monster[].class);

        if (monsterArray != null) {
            for (Monster m : monsterArray) {
                itemList.put(m.getId(), gson.toJson(m));
            }
        }
    }
}
