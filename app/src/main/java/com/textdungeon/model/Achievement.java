package com.textdungeon.model;

import com.google.gson.annotations.SerializedName;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Achievement {
    @SerializedName("id") private String id;
    @SerializedName("category") private String category;
    @SerializedName("name") private String name;
    @SerializedName("description") private String description;

    @SerializedName("targetType") private String targetType;
    @SerializedName("targetValue") private int targetValue;

    private int currentValue = 0;
    private boolean isUnlocked = false;
    private String achievedDate = null;

    public Achievement() {}

    public void unlock() {
        this.isUnlocked = true;
        this.currentValue = this.targetValue;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        this.achievedDate = sdf.format(new Date());
    }

    public String getId() { return id; }
    public String getCategory() { return category; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getTargetType() { return targetType; }
    public int getTargetValue() { return targetValue; }
    public int getCurrentValue() { return currentValue; }
    public boolean isUnlocked() { return isUnlocked; }
    public String getAchievedDate() { return achievedDate; }

    public void setCurrentValue(int currentValue) { this.currentValue = currentValue; }
    public void setUnlocked(boolean unlocked) { isUnlocked = unlocked; }
    public void setAchievedDate(String achievedDate) { this.achievedDate = achievedDate; }
}