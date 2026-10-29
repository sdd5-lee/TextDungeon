package com.textdungeon.ai;

import com.example.textdungeon.BuildConfig;

/**
 * API 키를 BuildConfig에서 읽어오는 유틸리티.
 * 게임은 클라우드를 쓰지 않아서 디버그 빌드의 측정 화면(클라우드 비교)만 쓴다. 출시 APK에는 키도 이 클래스도 없다
 */
public class ApiKeyManager {

    private ApiKeyManager() {}

    public static String getGeminiGodsKey() {
        return BuildConfig.GEMINI_API_KEY_GODS;
    }
}
