package com.textdungeon.system;

import android.content.Context;

import com.google.gson.Gson;
import com.textdungeon.player.Player;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

public class GameSave {
    private int score;
    private int currentFloor;
    private Player player;
    public GameSave(Player player) {
        this.player = player;
        this.score = 0;
        this.currentFloor = 1;
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
    public void setCurrentFloor(int floor) { this.currentFloor = floor; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public Player getPlayer() {
        return player;
    }
}