package com.textdungeon.system;

import android.content.Context;

import com.google.gson.Gson;
import com.textdungeon.player.Player;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

public class GameSave {
    private int currentFloor;
    private Player player;
    private UserRecord recode;
    public GameSave(Player player, int currentFloor,UserRecord userRecord) {
        this.player = player;
        this.currentFloor = currentFloor;
        this.recode = userRecord;
    }
    public GameSave(Player player,UserRecord userRecord) {
        this.player = player;
        this.recode = userRecord;
    }

    public void save(Context context) {
        Gson gson = new Gson();
        String json = gson.toJson(this);
        String fileName = "save.json";

        try (FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE)) {
            fos.write(json.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static GameSave load(Context context) {
        String fileName = "save.json";
        try (FileInputStream fis = context.openFileInput(fileName);
             InputStreamReader isr = new InputStreamReader(fis)) {
            return new Gson().fromJson(isr, GameSave.class);
        } catch (Exception e) {
            return null;
        }
    }

    public int getCurrentFloor() { return currentFloor; }
    public void setCurrentFloor(int floor) { currentFloor = floor; }

    public Player getPlayer() {
        return player;
    }

    public UserRecord getUserRecord() {
        return this.recode;
    }
}