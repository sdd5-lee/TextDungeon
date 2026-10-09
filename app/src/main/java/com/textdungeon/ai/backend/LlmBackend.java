package com.textdungeon.ai.backend;

/**
 * "프롬프트를 넣으면 텍스트가 나온다"만 책임지는 AI 백엔드.
 * 클라우드(Gemini)와 온디바이스 모델이 같은 인터페이스를 구현하므로,
 * 상위 코드(ChaosDice, EventGenerator)는 어떤 모델이 돌고 있는지 몰라도 된다.
 */
public interface LlmBackend {

    void generate(String prompt, ResponseFormat format, LlmCallback callback);

    /** 지금 바로 요청을 받을 수 있는지 (온디바이스: 모델 로드 완료 여부) */
    boolean isReady();

    /** 로그용 이름 */
    String name();
}
