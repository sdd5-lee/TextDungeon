package com.textdungeon.system;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.textdungeon.data.Difficulty;
import com.textdungeon.data.EventGson;
import com.textdungeon.event.GameEvent;
import com.textdungeon.model.ShopUpgrade;
import com.textdungeon.model.Stat;
import com.textdungeon.player.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class GameSave {
    private static final String RUN_FILE = "run_save.json";
    private static final String META_FILE = "user_record.json";
    private static final String TAG = "GameSave";
    private int currentFloor;
    private final Player player;
    private final Difficulty difficulty;
    private Map<Integer, GameEvent> aiEvents;
    /** 지금 화면에 떠 있는 이벤트. 저장하지 않으면 이어하기 할 때 다른 이벤트로 바뀐다(리롤 가능). */
    private GameEvent currentEvent;

    public GameSave(Player player, int currentFloor, Difficulty difficulty,
                    Map<Integer, GameEvent> aiEvents, GameEvent currentEvent) {
        this.player = player;
        this.currentFloor = currentFloor;
        this.difficulty = difficulty;
        this.aiEvents = aiEvents;
        this.currentEvent = currentEvent;
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
        newPlayer.getStat().setHp(newPlayer.getMaxHp());
        return newPlayer;
    }
    // ───────────── 파일 입출력 ─────────────
    // 저장은 임시 파일에 다 쓴 뒤 이름을 바꿔 교체한다. 쓰는 도중 앱이 죽어도 기존 세이브가 깨지지 않는다.
    // 불러오기에 실패한 파일은 지우지 않고 .corrupt 로 옮겨둔다 (덮어써서 영영 잃는 것 방지).

    private static boolean writeAtomic(Context context, String fileName, String json) {
        File dir = context.getFilesDir();
        File tmp = new File(dir, fileName + ".tmp");
        File target = new File(dir, fileName);
        try (FileOutputStream fos = new FileOutputStream(tmp)) {
            fos.write(json.getBytes(StandardCharsets.UTF_8));
            fos.getFD().sync();
        } catch (Exception e) {
            Log.e(TAG, fileName + " 저장 실패", e);
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
            return false;
        }
        if (!tmp.renameTo(target)) {
            Log.e(TAG, fileName + " 저장 파일 교체 실패");
            return false;
        }
        return true;
    }

    private static void keepCorruptFile(File file) {
        File backup = new File(file.getParentFile(), file.getName() + ".corrupt." + System.currentTimeMillis());
        if (file.renameTo(backup)) {
            Log.e(TAG, "손상된 세이브를 보관했습니다: " + backup.getName());
        }
    }

    public static boolean saveUserRecord(Context context, UserRecord record) {
        return writeAtomic(context, META_FILE, new Gson().toJson(record));
    }

    public static UserRecord loadUserRecord(Context context) {
        File file = new File(context.getFilesDir(), META_FILE);
        if (!file.exists()) return new UserRecord(); // 첫 실행
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
            UserRecord record = new Gson().fromJson(isr, UserRecord.class);
            return record != null ? record : new UserRecord();
        } catch (Exception e) {
            Log.e(TAG, "기록 불러오기 실패", e);
            keepCorruptFile(file);
            return new UserRecord();
        }
    }

    public boolean runSave(Context context) {
        // 이벤트가 BattleEvent/ShopEvent인지 구분해서 저장해야 하므로 EventGson 사용
        return writeAtomic(context, RUN_FILE, EventGson.create().toJson(this));
    }

    public static GameSave runLoad(Context context) {
        File file = new File(context.getFilesDir(), RUN_FILE);
        if (!file.exists()) return null;
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
            return EventGson.create().fromJson(isr, GameSave.class);
        } catch (Exception e) {
            Log.w(TAG, "새 형식으로 불러오기 실패, 예전 형식으로 재시도", e);
        }
        // 업데이트 전 세이브는 이벤트 종류(type) 없이 저장돼 있으므로 예전 방식으로 읽는다 (진행 중인 판 유실 방지)
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
            return new Gson().fromJson(isr, GameSave.class);
        } catch (Exception e) {
            Log.e(TAG, "진행 중인 게임 불러오기 실패", e);
            keepCorruptFile(file);
            return null;
        }
    }
    public static void deleteRun(Context context){
        File file = new File(context.getFilesDir(), RUN_FILE);
        if (file.exists()){
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    public int getCurrentFloor() { return currentFloor; }

    public Player getPlayer() {
        return player;
    }
    public Difficulty getDifficulty() {
        return difficulty;
    }
    public Map<Integer, GameEvent> getAiEvents() {
        return aiEvents;
    }
    public GameEvent getCurrentEvent() {
        return currentEvent;
    }
}