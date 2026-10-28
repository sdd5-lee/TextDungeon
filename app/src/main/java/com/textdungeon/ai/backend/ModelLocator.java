package com.textdungeon.ai.backend;

import android.content.Context;

import java.io.File;

/**
 * 온디바이스 모델 파일을 찾는다. 앞에서부터 찾고, 없으면 null.
 *  1. 첫 실행 때 받은 모델: 3단계에서 다운로드를 만들 때 추가한다
 *  2. 개발용 경로 getExternalFilesDir(null)/models/ : adb push로 넣는다
 */
public final class ModelLocator {
    /** 0-A에서 고른 모델 (Gemma 4 E2B, 2.58GB) */
    public static final String MODEL_FILE = "gemma-4-E2B-it.litertlm";

    private ModelLocator() {}

    public static File find(Context context) {
        // 1. 첫 실행 다운로드 위치 (3단계)

        // 2. 개발용
        File external = context.getExternalFilesDir(null);
        if (external != null) {
            File dev = new File(new File(external, "models"), MODEL_FILE);
            if (dev.isFile() && dev.length() > 0) return dev;
        }
        return null;
    }
}
