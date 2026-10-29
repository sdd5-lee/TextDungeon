package com.textdungeon.ai;

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;

import com.textdungeon.ai.backend.ModelDownloader;
import com.textdungeon.ai.backend.OnDeviceBackend;
import com.textdungeon.ai.backend.OnDeviceDebugFlags;
import com.textdungeon.model.Item;
import com.textdungeon.model.Monster;

import java.util.List;

/**
 * AI 기능의 진입점. 어떤 기능이 어떤 백엔드를 쓰는지는 여기서만 결정한다.
 *
 *  - 이벤트 생성: 온디바이스 틀 방식(TemplateEventGenerator). 모델이 없거나 실패하면 그 판은 AI 이벤트 없이 간다
 *  - 혼돈의 주사위(선택지 추가): 온디바이스, 준비 안 됐거나 실패하면 결과표(ChaosTable)
 *
 * 게임은 클라우드(Gemini)를 쓰지 않는다. GeminiBackend와 JSON 방식(EventGenerator)은 디버그 측정 화면의 비교용으로만 남아 있다.
 *
 * 온디바이스 모델은 생성자에서 올리지 않는다. 게임을 시작할 때 warmUpOnDevice(),
 * 판이 끝날 때 releaseOnDevice()를 부른다. 모델 파일은 메인 화면에서 startModelDownload()로 받는다.
 */
public class AiManager {
    /** 디버그 측정 화면에서 비교용으로 부르는 클라우드 모델 */
    static final String GEMINI_MODEL = "gemini-3-flash-preview";

    private final OnDeviceBackend onDeviceBackend;
    private final ModelDownloader modelDownloader;
    private final TemplateEventGenerator eventGenerator;
    private final ChaosDice chaosDice;
    // 게임 중이라 모델이 올라가 있어야 하는지 (warmUp 이후 release 전). 받기가 끝났을 때 바로 올릴지 정한다
    private volatile boolean onDeviceWanted;

    public AiManager(Context appContext) {
        this.onDeviceBackend = new OnDeviceBackend(appContext);
        this.modelDownloader = new ModelDownloader(appContext, () -> {
            if (onDeviceWanted) onDeviceBackend.warmUp();
        });

        this.eventGenerator = new TemplateEventGenerator(onDeviceBackend);
        this.chaosDice = new ChaosDice(onDeviceBackend,
                () -> OnDeviceDebugFlags.timeoutMs(appContext, AiTimeouts.ON_DEVICE_MS));

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

    public void requestChaosChoice(ChaosDice.Request request, ChaosDice.Callback callback) {
        chaosDice.roll(request, callback);
    }

    /**
     * 신의 이벤트 하나를 온디바이스 모델로 만든다. 모델은 한 번에 요청 하나만 받으므로 앞 요청이 끝난 뒤에 부른다.
     * 모델이 준비되지 않았으면 바로 onError로 간다
     */
    public void generateEvent(int floor, AiType god, List<Item> nearItems, List<Item> treasureItems,
                              List<Monster> monsters, int rewardMultiplier, int expMultiplier, AiCallback callback) {
        eventGenerator.generate(floor, god, nearItems, treasureItems, monsters, rewardMultiplier, expMultiplier, callback);
    }

    /** 오래 걸려 포기한 이벤트 생성을 멈춘다. 멈춘 요청의 콜백은 늦게라도 온다 */
    public void cancelOnDevice() {
        onDeviceBackend.cancel();
    }

    /** 온디바이스 모델을 뒤에서 올린다. 이미 올라가 있거나 쓸 수 없는 기기면 아무것도 하지 않는다 */
    public void warmUpOnDevice() {
        onDeviceWanted = true;
        onDeviceBackend.warmUp();
    }

    /** 온디바이스 모델을 내린다. 추론 중이면 끝난 뒤에 닫는다 */
    public void releaseOnDevice() {
        onDeviceWanted = false;
        onDeviceBackend.release();
    }

    /** 모델 파일이 없으면 Wi-Fi에서 받기 시작한다. 메인 화면에 올 때마다 부른다 */
    public void startModelDownload() {
        modelDownloader.start();
    }

    public boolean isModelDownloadEnabled() {
        return modelDownloader.isEnabled();
    }

    /** 설정 화면 스위치. 끄면 모델을 내리고 받아 둔 파일을 지워서, 주사위는 결과표로만 간다 */
    public void setModelDownloadEnabled(boolean enabled) {
        if (!enabled) releaseOnDevice();
        modelDownloader.setEnabled(enabled);
        // 게임 중에 다시 켜고 모델이 이미 있으면(adb push) 다음 warmUp에서 바로 올라간다
    }

    public boolean isOnDeviceSupported() {
        return modelDownloader.isSupported();
    }

    public boolean hasOnDeviceModel() {
        return modelDownloader.hasModel();
    }

    public OnDeviceBackend.State getOnDeviceState() {
        return onDeviceBackend.getState();
    }

    /** 설정 화면의 "온디바이스 AI: …" 한 줄 */
    public String getOnDeviceStatusLine() {
        ModelDownloader.Progress p = modelDownloader.progress();
        return onDeviceStatusLine(p.status, p.percent, onDeviceBackend.getState());
    }

    static String onDeviceStatusLine(ModelDownloader.Status status, int percent, OnDeviceBackend.State state) {
        String text;
        switch (status) {
            case READY:
                text = state == OnDeviceBackend.State.FAILED ? "사용 불가 (이 기기에서 실행 실패)" : "준비됨";
                break;
            case DOWNLOADING:
                text = percent >= 0 ? "받는 중 " + percent + "%" : "받는 중";
                break;
            case WAITING_NETWORK:
                text = "받는 중 (Wi-Fi 연결 대기)";
                break;
            case VERIFYING:
                text = "받는 중 (파일 확인)";
                break;
            case NO_SPACE:
                text = "사용 불가 (저장 공간 3.7GB 필요)";
                break;
            case FAILED:
                text = "사용 불가 (받기 실패)";
                break;
            case UNSUPPORTED:
                text = "사용 불가 (지원하지 않는 기기)";
                break;
            case DISABLED:
                text = "꺼짐 (신들의 이벤트 없음, 주사위는 준비된 결과)";
                break;
            default:
                text = "받기 전";
                break;
        }
        return "온디바이스 AI: " + text;
    }

    public boolean isOnDeviceReady() {
        return onDeviceBackend.isReady();
    }
}
