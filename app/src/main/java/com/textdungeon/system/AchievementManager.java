package com.textdungeon.system;

import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.textdungeon.model.Achievement;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AchievementManager {
    private List<Achievement> achievements = new ArrayList<>();
    private final Context context;

    public AchievementManager(Context context) {
        this.context = context;
        loadBaseAchievements();
    }

    private void loadBaseAchievements() {
        try {
            InputStream is = context.getAssets().open("achievements_list.json");
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<Achievement>>(){}.getType();
            achievements = gson.fromJson(new InputStreamReader(is), listType);
        } catch (Exception e) {
            Log.e("AchievementManager", "기본 업적 뼈대 로드 실패", e);
        }
    }

    public void syncSavedData(List<Achievement> savedList) {
        if (savedList == null || savedList.isEmpty()) return;

        for (Achievement saved : savedList) {
            for (Achievement base : achievements) {
                if (base.getId().equals(saved.getId())) {
                    base.setCurrentValue(saved.getCurrentValue());
                    base.setUnlocked(saved.isUnlocked());
                    base.setAchievedDate(saved.getAchievedDate());
                }
            }
        }
    }

    public List<Achievement> getAllAchievements() { return achievements; }

    // amount: 이번에 들어온 값, isAccumulated: 누적형(true)이냐 단발성 갱신(false)이냐
    public List<Achievement> updateProgress(String targetType, int amount, boolean isAccumulated) {
        List<Achievement> unlockedNow = new ArrayList<>();

        for (Achievement ach : achievements) {
            if (ach.isUnlocked()) continue;

            if (ach.getTargetType().equals(targetType)) {
                if (isAccumulated) {
                    ach.setCurrentValue(ach.getCurrentValue() + amount);
                } else {
                    if (amount > ach.getCurrentValue()) ach.setCurrentValue(amount);
                }
                if (ach.getCurrentValue() >= ach.getTargetValue()) {
                    ach.unlock();
                    unlockedNow.add(ach);
                }
            }
        }
        return unlockedNow;
    }
}