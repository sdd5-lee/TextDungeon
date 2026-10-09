package com.textdungeon.ai;

import com.textdungeon.ai.backend.FallbackBackend;
import com.textdungeon.ai.backend.GeminiBackend;
import com.textdungeon.ai.backend.LlmBackend;
import com.textdungeon.ai.backend.OnDeviceBackend;
import com.textdungeon.event.GameEvent;
import com.textdungeon.model.Item;
import com.textdungeon.model.Monster;
import com.textdungeon.model.Stat;

import java.util.List;

/**
 * AI 기능의 진입점. 어떤 기능이 어떤 백엔드를 쓰는지는 여기서만 결정한다.
 *
 *  - 이벤트 생성: 클라우드(Gemini)
 *  - 혼돈의 주사위(선택지 추가): 온디바이스 우선, 준비 안 됐거나 실패하면 클라우드
 *
 * 외부에서 쓰는 메서드 시그니처는 예전과 같아서 Activity 쪽은 바꿀 필요가 없다.
 */
public class AiManager {
    private static final String GEMINI_MODEL = "gemini-3-flash-preview";

    private final OnDeviceBackend onDeviceBackend;
    private final EventGenerator generator;
    private final ChaosDice chaosDice;

    public AiManager() {
        LlmBackend eventCloud = new GeminiBackend(GEMINI_MODEL, ApiKeyManager.getGeminiGodsKey());
        LlmBackend choiceCloud = new GeminiBackend(GEMINI_MODEL, ApiKeyManager.getGeminiKey());
        this.onDeviceBackend = new OnDeviceBackend();

        this.generator = new EventGenerator(eventCloud);
        this.chaosDice = new ChaosDice(new FallbackBackend(onDeviceBackend, choiceCloud));
    }

    public void requestChaosChoice(int floor, Stat stat, List<Item> itemList, GameEvent currentEvent, AiCallback callback) {
        chaosDice.roll(floor, stat, itemList, currentEvent, callback);
    }

    public void generate(int targetFloor, Stat stat, List<Item> all, List<Monster> monsterList,
                         String eventType, AiType aiType, AiCallback aiCallback) {
        generator.generate(targetFloor, stat, all, monsterList, eventType, aiType, aiCallback);
    }

    /** 2단계에서 모델 로딩 상태를 UI에 보여줄 때 사용 */
    public boolean isOnDeviceReady() {
        return onDeviceBackend.isReady();
    }
}
