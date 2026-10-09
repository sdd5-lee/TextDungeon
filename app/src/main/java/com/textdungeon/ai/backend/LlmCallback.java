package com.textdungeon.ai.backend;

/** 백엔드 응답 콜백. 어느 스레드에서 호출될지는 보장하지 않는다(호출부에서 메인 스레드로 넘길 것). */
public interface LlmCallback {
    void onResult(String text);
    void onError(String errorMessage);
}
