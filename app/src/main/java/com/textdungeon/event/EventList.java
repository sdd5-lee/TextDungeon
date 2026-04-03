package com.textdungeon.event;

import android.content.Context;

import com.google.gson.Gson;
import com.textdungeon.model.Item;
import com.textdungeon.model.Monster;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class EventList {
    private static Map<String, String> eventList = new HashMap<>();

    public static GameEvent getEvent(String id) {
        String json = eventList.get(id);
        return GameEvent.createFromJson(json);
    }

    public static void init(Context context) {
        String fileName = "event_list.json";

        try (InputStream is = context.getAssets().open(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            parseeEventList(sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void parseeEventList(String json) {
        Gson gson = new Gson();

        GameEvent[] eventArray = gson.fromJson(json, GameEvent[].class);

        if (eventArray != null) {
            for (GameEvent e : eventArray) {
                eventList.put(e.getId(), gson.toJson(e));
            }
        }
    }
}
