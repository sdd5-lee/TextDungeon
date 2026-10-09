package com.textdungeon.ai.backend;

/** 백엔드에 요청할 출력 형태. 온디바이스 백엔드는 JSON 강제 기능이 없으면 무시해도 된다. */
public enum ResponseFormat {
    TEXT,
    JSON
}
