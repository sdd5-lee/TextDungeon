package com.textdungeon.ai.backend;

/**
 * 온디바이스 모델 자리.
 * 2단계(모델 선정)에서 MediaPipe/LiteRT-LM 등으로 실제 구현을 채운다.
 * 지금은 isReady()가 false라서 FallbackBackend가 항상 클라우드로 넘긴다.
 *
 * 구현 시 주의:
 *  - 모델 로드는 수 초 걸리고 RAM을 계속 점유하므로 앱 전체에서 인스턴스 하나만 유지할 것
 *  - 로드가 끝나기 전에는 isReady()가 false를 반환해야 한다
 *  - 추론은 메인 스레드가 아닌 곳에서 돌릴 것
 */
public class OnDeviceBackend implements LlmBackend {

    @Override
    public void generate(String prompt, ResponseFormat format, LlmCallback callback) {
        callback.onError("온디바이스 모델이 아직 준비되지 않았습니다.");
    }

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public String name() {
        return "OnDevice(미구현)";
    }
}
