package com.textdungeon.ai;

/** AI 대기 시간. 근거가 되는 측정값과 함께 여기 한 곳에만 둔다. */
public final class AiTimeouts {

    /**
     * 온디바이스 응답을 기다리는 시간. 넘으면 추론을 멈추고 클라우드로 넘긴다.
     * 0-A 에뮬레이터 CPU 최대 4.9초. 0-B(실기기 응답의 느린 쪽 10%)를 재면 그보다 넉넉하게 다시 정한다.
     */
    public static final long ON_DEVICE_MS = 8_000;

    /**
     * 주사위 전체 대기 시간. 넘으면 오류로 끝내고 주사위 횟수를 돌려준다.
     * 온디바이스 8초 + 0-A 클라우드 응답의 느린 쪽 10%(23.4초) + 여유.
     * 계획서 초안은 20초였지만 그러면 클라우드 응답 20건 중 6건이 잘린다.
     */
    public static final long DICE_TOTAL_MS = 35_000;

    private AiTimeouts() {}
}
