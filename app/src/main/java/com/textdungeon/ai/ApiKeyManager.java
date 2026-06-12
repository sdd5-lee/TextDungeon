package com.textdungeon.ai;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.textdungeon.BuildConfig;

public class ApiKeyManager {
    private static ApiKeyManager instance;
    private final String geminiApiKey;
    private final String geminiApiKeyGods;

    private ApiKeyManager(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("ApiKeys", Context.MODE_PRIVATE);
        geminiApiKey = prefs.getString("GEMINI_API_KEY", BuildConfig.GEMINI_API_KEY);
        geminiApiKeyGods = prefs.getString("GEMINI_API_KEY_GODS", BuildConfig.GEMINI_API_KEY_GODS);
    }

    public static void init(Context context) {
        if (instance == null) {
            // 메모리 누수를 방지하기 위해 ApplicationContext를 사용하는 것이 안전합니다.
            instance = new ApiKeyManager(context.getApplicationContext());
        }
    }

    // 방어 로직을 추가한 헬퍼 메서드
    private static void checkInitialization() {
        if (instance == null) {
            throw new IllegalStateException("ApiKeyManager가 초기화되지 않았습니다. 사용하기 전에 반드시 ApiKeyManager.init(context)를 먼저 호출하세요.");
        }
    }

    public static String getGeminiKey() {
        checkInitialization();
        return instance.geminiApiKey;
    }

    public static String getGeminiGodsKey() {
        checkInitialization();
        return instance.geminiApiKeyGods;
    }
}