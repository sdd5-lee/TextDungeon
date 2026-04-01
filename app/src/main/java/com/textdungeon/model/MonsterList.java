package com.textdungeon.model;

import android.content.Context;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class MonsterList {
    private static Map<String, String> monsterList = new HashMap<>();

    public static Monster spawn(String id) {
        String json = monsterList.get(id);
        return Monster.createFromJson(json);
    }

    public static void init(Context context) {
        String fileName = "monster_list.json";

        try (InputStream is = context.getAssets().open(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            parseMonsterList(sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void parseMonsterList(String json) {
        Gson gson = new Gson();

        Monster[] monsterArray = gson.fromJson(json, Monster[].class);

        if (monsterArray != null) {
            for (Monster m : monsterArray) {
                monsterList.put(m.getId(), gson.toJson(m));
            }
        }
    }
}
