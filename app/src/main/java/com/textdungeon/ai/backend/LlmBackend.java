package com.textdungeon.ai.backend;

/**
 * "프롬프트를 넣으면 텍스트가 나온다"만 책임지는 AI 백엔드.
 * 클라우드(Gemini)와 온디바이스 모델이 같은 인터페이스를 구현하므로,
 * 상위 코드(ChaosDice, EventGenerator)는 어떤 모델이 돌고 있는지 몰라도 된다.
 */
public interface LlmBackend {

    void generate(String prompt, ResponseFormat format, LlmCallback callback);

    /**
     * 응답을 validator로 검사해서 통과한 것만 onResult로 넘기고, 통과하지 못하면 onError로 넘긴다.
     * FallbackBackend는 우선 백엔드의 응답이 검사를 통과하지 못하면 예비 백엔드로 다시 요청한다.
     */
    default void generate(String prompt, ResponseFormat format, ResponseValidator validator, LlmCallback callback) {
        generate(prompt, format, validator.guard(callback));
    }

    /** 진행 중인 요청을 멈춘다. 멈출 수 없는 백엔드(클라우드)는 아무것도 하지 않는다 */
    default void cancel() {}

    /** 지금 바로 요청을 받을 수 있는지 (온디바이스: 모델 로드 완료 여부) */
    boolean isReady();

    /** 로그용 이름 */
    String name();
}
