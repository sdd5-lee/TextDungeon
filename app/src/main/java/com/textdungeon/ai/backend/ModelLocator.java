package com.textdungeon.ai.backend;

import android.content.Context;

import java.io.File;

/**
 * 온디바이스 모델 파일을 찾는다. 없으면 null.
 * 위치는 getExternalFilesDir(null)/models/ 하나다.
 *  - 출시: 첫 실행 때 ModelDownloader가 받아서 확인을 마친 뒤 이 이름으로 둔다
 *  - 개발: adb push로 넣는다 (넣어 두면 ModelDownloader는 받지 않는다)
 * 자동 백업에서는 뺀다 (res/xml의 backup_rules, data_extraction_rules).
 */
public final class ModelLocator {
    /** 0-A에서 고른 모델 (Gemma 4 E2B, 2.58GB) */
    public static final String MODEL_FILE = "gemma-4-E2B-it.litertlm";

    private ModelLocator() {}

    public static File find(Context context) {
        File model = target(context);
        if (model != null && model.isFile() && model.length() > 0) return model;
        return null;
    }

    /** 모델을 둘 자리. 외부 저장소를 쓸 수 없으면 null */
    static File target(Context context) {
        File external = context.getExternalFilesDir(null);
        if (external == null) return null;
        return new File(new File(external, "models"), MODEL_FILE);
    }
}
