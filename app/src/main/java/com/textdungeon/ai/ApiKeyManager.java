package com.textdungeon.ai;

import com.example.textdungeon.BuildConfig;

/**
 * API 키를 BuildConfig에서 읽어오는 유틸리티.
 * (관리자 패널을 통한 런타임 키 입력/오버라이드 기능은 제거됨 — 항상 빌드 시점 키만 사용)
 */
public class ApiKeyManager {

    private ApiKeyManager() {}

    public static String getGeminiKey() {
        return BuildConfig.GEMINI_API_KEY;
    }

    public static String getGeminiGodsKey() {
        return BuildConfig.GEMINI_API_KEY_GODS;
    }
}
