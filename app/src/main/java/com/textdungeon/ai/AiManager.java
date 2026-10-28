package com.textdungeon.ai;

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;

import com.textdungeon.ai.backend.FallbackBackend;
import com.textdungeon.ai.backend.GeminiBackend;
import com.textdungeon.ai.backend.LlmBackend;
import com.textdungeon.ai.backend.OnDeviceBackend;
import com.textdungeon.ai.backend.OnDeviceDebugFlags;
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
 * 온디바이스 모델은 생성자에서 올리지 않는다. 게임을 시작할 때 warmUpOnDevice(),
 * 판이 끝날 때 releaseOnDevice()를 부른다.
 */
public class AiManager {
    static final String GEMINI_MODEL = "gemini-3-flash-preview";

    private final OnDeviceBackend onDeviceBackend;
    private final EventGenerator generator;
    private final ChaosDice chaosDice;

    public AiManager(Context appContext) {
        LlmBackend eventCloud = new GeminiBackend(GEMINI_MODEL, ApiKeyManager.getGeminiGodsKey());
        LlmBackend choiceCloud = new GeminiBackend(GEMINI_MODEL, ApiKeyManager.getGeminiKey());
        this.onDeviceBackend = new OnDeviceBackend(appContext);

        this.generator = new EventGenerator(eventCloud);
        this.chaosDice = new ChaosDice(new FallbackBackend(onDeviceBackend, choiceCloud,
                () -> OnDeviceDebugFlags.timeoutMs(appContext, AiTimeouts.ON_DEVICE_MS)));

        // Application 클래스가 없어서 여기서 등록한다. 모델이 1~2GB를 차지하므로
        // 메모리 회수 대상(TRIM_MEMORY_BACKGROUND 이상)이 되면 내린다.
        // UI_HIDDEN은 앱을 잠깐 전환하거나 화면을 끌 때도 와서, 그때마다 다시 올리느라 몇 초씩 쓰게 된다
        appContext.registerComponentCallbacks(new ComponentCallbacks2() {
            @Override
            public void onTrimMemory(int level) {
                if (level >= TRIM_MEMORY_BACKGROUND) releaseOnDevice();
            }

            @Override
            public void onConfigurationChanged(Configuration newConfig) {}

            @Override
            public void onLowMemory() {
                releaseOnDevice();
            }
        });
    }

    /**
     * @param rewardMultiplier 난이도 보상 배수 (Difficulty.rewardMultiplier)
     * @param expMultiplier    특성 경험치 배수 (Reward.expMultiplier(player))
     */
    public void requestChaosChoice(int floor, Stat stat, List<Item> itemList, GameEvent currentEvent,
                                   int rewardMultiplier, int expMultiplier, AiCallback callback) {
        chaosDice.roll(floor, stat, itemList, currentEvent, rewardMultiplier, expMultiplier, callback);
    }

    public void generate(int targetFloor, Stat stat, List<Item> all, List<Monster> monsterList,
                         String eventType, AiType aiType, AiCallback aiCallback) {
        generator.generate(targetFloor, stat, all, monsterList, eventType, aiType, aiCallback);
    }

    /** 온디바이스 모델을 뒤에서 올린다. 이미 올라가 있거나 쓸 수 없는 기기면 아무것도 하지 않는다 */
    public void warmUpOnDevice() {
        onDeviceBackend.warmUp();
    }

    /** 온디바이스 모델을 내린다. 추론 중이면 끝난 뒤에 닫는다 */
    public void releaseOnDevice() {
        onDeviceBackend.release();
    }

    /** 3단계에서 모델 상태를 설정 화면에 보여줄 때 사용 */
    public OnDeviceBackend.State getOnDeviceState() {
        return onDeviceBackend.getState();
    }

    public boolean isOnDeviceReady() {
        return onDeviceBackend.isReady();
    }
}
