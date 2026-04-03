package com.textdungeon.data;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class DataControl<D> {
    private final Map<String, String> dataList = new HashMap<>();
    private final Class<D> clazz;
    private final Gson gson = new Gson();

    public DataControl(Class<D> clazz) {
        this.clazz = clazz;
    }
    public void init(Context context,String fileName) {

        try (BufferedReader bf= new BufferedReader(new InputStreamReader(context.getAssets().open(fileName)))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bf.readLine()) != null){
                sb.append(line);
            }
            JsonArray array = JsonParser.parseString(sb.toString()).getAsJsonArray();
            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                String id = obj.get("id").getAsString();
                dataList.put(id, obj.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public D spawn(String id) {
        String json = dataList.get(id);
        if (json == null) return null;
        return gson.fromJson(json, clazz);
    }
}
