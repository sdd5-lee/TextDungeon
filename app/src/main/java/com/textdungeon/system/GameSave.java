package com.textdungeon.system;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.textdungeon.data.Difficulty;
import com.textdungeon.event.GameEvent;
import com.textdungeon.model.ShopUpgrade;
import com.textdungeon.model.Stat;
import com.textdungeon.player.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.Map;

public class GameSave {
    private static final String RUN_FILE = "run_save.json";
    private static final String META_FILE = "user_record.json";
    private int currentFloor;
    private final Player player;
    private final Difficulty difficulty;
    private Map<Integer, GameEvent> aiEvents;

    public GameSave(Player player, int currentFloor, Difficulty difficulty,Map<Integer, GameEvent> aiEvents) {
        this.player = player;
        this.currentFloor = currentFloor;
        this.difficulty = difficulty;
        this.aiEvents = aiEvents;
    }
    public static Player createNewPlayer(UserRecord record, String name, com.textdungeon.model.Job job, String traitName){
        Player newPlayer = new Player(name,job);
        Stat stat = newPlayer.getStat();

        com.textdungeon.model.Trait customTrait = com.textdungeon.model.Trait.valueOf(traitName);
        if (customTrait.modifyBaseStat()) {
            int bonus = customTrait.modifyStatBonus();
            stat.addStrength(bonus);
            stat.addAgility(bonus);
            stat.addHealth(bonus);
            stat.addWisdom(bonus);
        }
        for (ShopUpgrade upgrade : ShopUpgrade.values()) {
            int currentLevel = record.getUpgradeLevel(upgrade.name());
            if (currentLevel <= 0) continue;
            int totalBonus = currentLevel * upgrade.valuePerLevel;

            switch (upgrade.category) {
                case "STR": stat.addStrength(totalBonus); break;
                case "AGI": stat.addAgility(totalBonus); break;
                case "HEALTH": stat.addHealth(totalBonus); break;
                case "WIS": stat.addWisdom(totalBonus); break;
                case "STAT_POINT": stat.addStatPoint(totalBonus); break;
                case "GOLD": stat.addGold(totalBonus); break;
                case "DICE_Chane": newPlayer.addDiceChane(totalBonus); break;
            }
        }
        stat.updateBattleStat(newPlayer.getLevel());
        return newPlayer;
    }
    public static void saveUserRecord(Context context, UserRecord record) {
        try (FileOutputStream fos = context.openFileOutput(META_FILE, Context.MODE_PRIVATE)) {
            fos.write(new Gson().toJson(record).getBytes());
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static UserRecord loadUserRecord(Context context) {
        try (FileInputStream fis = context.openFileInput(META_FILE);
             InputStreamReader isr = new InputStreamReader(fis)) {
            return new Gson().fromJson(isr, UserRecord.class);
        } catch (Exception e) {
            return new UserRecord();
        }
    }
    public void runSave(Context context) {
        Gson gson = new Gson();
        String json = gson.toJson(this);

        try (FileOutputStream fos = context.openFileOutput(RUN_FILE, Context.MODE_PRIVATE)) {
            fos.write(json.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static GameSave runLoad(Context context) {
        try (FileInputStream fis = context.openFileInput(RUN_FILE);
             InputStreamReader isr = new InputStreamReader(fis)) {
            return new Gson().fromJson(isr, GameSave.class);
        } catch (Exception e) {
            return null;
        }
    }
    public static void deleteRun(Context context){
        File file = new File(context.getFilesDir(), RUN_FILE);
        if (file.exists()){
            file.delete();
        }
    }

    public int getCurrentFloor() { return currentFloor; }
    public void setCurrentFloor(int floor) { currentFloor = floor; }

    public Player getPlayer() {
        return player;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }
}