package com.textdungeon.system;

import android.content.Context;

import com.google.gson.Gson;
import com.textdungeon.player.Player;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class GameSave {
    private int slotNum;
    private int score;
    private int currentFloor; // 현재 층수
    private Player player;
    public GameSave(int slotNum, Player player) {
        this.slotNum = slotNum;
        this.player = player;
        this.score = 0;
        this.currentFloor = 1; // 기본 1층 시작
    }

    public void save(Context context) {
        Gson gson = new Gson();
        String json = gson.toJson(this);
        String fileName = "save_slot_" + slotNum + ".json";

        try (FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE)) {
            fos.write(json.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static GameSave load(Context context, int slotNum) {
        String fileName = "save_slot_" + slotNum + ".json";
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