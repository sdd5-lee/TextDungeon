package com.textdungeon.ai.backend;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import com.example.textdungeon.BuildConfig;
import com.google.ai.edge.litertlm.Backend;
import com.google.ai.edge.litertlm.Content;
import com.google.ai.edge.litertlm.Conversation;
import com.google.ai.edge.litertlm.ConversationConfig;
import com.google.ai.edge.litertlm.Engine;
import com.google.ai.edge.litertlm.EngineConfig;
import com.google.ai.edge.litertlm.Message;
import com.google.ai.edge.litertlm.SamplerConfig;
import com.google.ai.edge.litertlm.ThinkingConfig;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * 온디바이스 모델 (LiteRT-LM, Gemma 4 E2B). 혼돈의 주사위가 결과표보다 먼저 쓴다.
 *
 * 로딩, 추론, 해제는 모두 전용 스레드 하나에서 처리한다 (엔진을 메인 스레드에서 부르지 않는다).
 * 앱을 켤 때가 아니라 게임을 시작할 때 warmUp()으로 올리고, 판이 끝나거나 메모리가 부족하면 release()로 내린다.
 * 준비되지 않았거나 앞 요청을 처리 중이면 isReady()가 false라서 ChaosDice가 결과표를 쓴다.
 */
public class OnDeviceBackend implements LlmBackend {
    private static final String TAG = "OnDeviceBackend";

    public enum State {
        /** 로드 전이거나 해제됨 */
        NOT_LOADED,
        LOADING,
        READY,
        /** 모델 파일이 없거나 기기 조건 미달. 로드를 시도하지 않는다 (파일이 없던 경우만 warmUp 때 다시 본다) */
        UNAVAILABLE,
        /** 초기화 실패. 다시 시도하면 매번 몇 초씩 버리므로 프로세스가 살아 있는 동안 유지한다 */
        FAILED
    }

    /** 주사위 출력 상한. 이벤트처럼 긴 출력은 generate(..., maxOutputTokens, temperature, ...)로 따로 정한다 */
    private static final int MAX_OUTPUT_TOKENS = 200;
    // 온도를 정한 요청의 나머지 샘플링 값. S24에서 틀 방식 이벤트 20개를 잰 값 (docs/ondevice-event)
    private static final int SAMPLER_TOP_K = 40;
    private static final double SAMPLER_TOP_P = 0.95;
    // 0-A 측정과 같은 값. 주사위 프롬프트(600자 안팎)와 출력 200토큰, 이벤트(JSON 방식 프롬프트 약 2500자 + 출력)도 들어간다
    private static final int MAX_NUM_TOKENS = 4096;
    // 램이 이보다 적으면 쓰지 않는다. "6GB" 기기는 totalMem이 이보다 조금 작게 나오므로 사실상 8GB 이상. 0-B 결과로 조정
    private static final long MIN_TOTAL_RAM = 6L << 30;
    // CPU 백엔드는 cacheDir에 모델 크기만 한 캐시(Gemma 약 800MB)를 쓴다.
    // 공간이 모자라면 네이티브 코드가 abort해서 예외로도 잡을 수 없으므로 로드 전에 확인한다
    private static final long MIN_FREE_CACHE = 1L << 30;
    private static final Pattern THINK_BLOCK = Pattern.compile("(?s)<think>.*?(</think>|$)");

    private final Context appContext;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ondevice-llm");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean busy = new AtomicBoolean(false);

    private volatile State state = State.NOT_LOADED;
    // UNAVAILABLE의 이유가 모델 파일이 없어서인지. 받기가 끝나면 다시 로드할 수 있다
    private volatile boolean missingModel;
    private volatile String device = "";            // 실제로 뜬 장치 (GPU/CPU), 로그용
    private Engine engine;                          // executor 스레드에서만 읽고 쓴다
    private volatile Conversation running;          // cancel()이 다른 스레드에서 읽는다
    private volatile boolean cancelRequested;

    public OnDeviceBackend(Context appContext) {
        this.appContext = appContext.getApplicationContext();
    }

    public State getState() {
        return state;
    }

    /**
     * NOT_LOADED일 때 로드를 시작한다. 모델 파일이 없어서 UNAVAILABLE이었으면 파일이 생겼는지 다시 본다
     * (첫 실행 다운로드). 그 밖의 상태에서는 아무것도 하지 않는다
     */
    public void warmUp() {
        synchronized (this) {
            boolean retry = state == State.UNAVAILABLE && missingModel;
            if (state != State.NOT_LOADED && !retry) return;
            state = State.LOADING;
        }
        executor.execute(this::load);
    }

    /**
     * 엔진을 내린다. 추론 중이면 끝난 뒤에 닫는다 (같은 스레드에서 차례로 처리).
     * 새 요청은 바로 결과표로 가도록 상태부터 바꾼다. FAILED, UNAVAILABLE은 그대로 둔다.
     */
    public void release() {
        synchronized (this) {
            if (state != State.READY && state != State.LOADING) return;
            state = State.NOT_LOADED;
        }
        executor.execute(this::closeEngine);
    }

    @Override
    public void generate(String prompt, ResponseFormat format, LlmCallback callback) {
        generate(prompt, format, MAX_OUTPUT_TOKENS, null, callback);
    }

    @Override
    public void generate(String prompt, ResponseFormat format, int maxOutputTokens, Double temperature,
                         LlmCallback callback) {
        if (state != State.READY || !busy.compareAndSet(false, true)) {
            callback.onError("온디바이스 모델을 쓸 수 없음 (" + state + ")");
            return;
        }
        cancelRequested = false;
        executor.execute(() -> {
            String text = null;
            String error = null;
            try {
                if (engine == null) {
                    error = "온디바이스 모델이 해제됨";
                } else {
                    text = infer(prompt, maxOutputTokens, temperature);
                    if (text == null) error = "온디바이스 추론을 멈춤";
                }
            } catch (Throwable t) {
                if (cancelRequested) {
                    // cancelProcess()로 멈추면 sendMessage가 CANCELLED 예외를 던진다
                    Log.i(TAG, "온디바이스 추론을 멈춤: " + t.getMessage());
                    error = "온디바이스 추론을 멈춤";
                } else {
                    // 초기화는 됐는데 추론마다 실패하는 기기가 있다 (에뮬레이터 GPU: 초기화 4분 41초 뒤
                    // "Can not find OpenCL library"로 매번 5~7초 만에 실패). 주사위마다 시간을 버리지 않게 내리고 FAILED로 둔다
                    Log.e(TAG, "온디바이스 추론 실패 → 이번 실행 동안 쓰지 않음", t);
                    markFailed();
                    error = "온디바이스 추론 실패: " + t.getMessage();
                }
            }
            // 콜백 안에서 바로 다음 요청을 보내도 받을 수 있게 먼저 푼다 (이벤트는 하나씩 차례로 만든다)
            busy.set(false);
            if (error != null) callback.onError(error);
            else callback.onResult(text);
        });
    }

    /** @return 정리한 응답. 중간에 멈췄으면 null */
    private String infer(String prompt, int maxOutputTokens, Double temperature) throws Exception {
        debugDelay();
        if (cancelRequested) return null;
        // 온도를 정하지 않으면 모델 기본값. 씨앗은 요청마다 바꾼다 (같은 프롬프트로 다시 시도하면 다른 답이 나오게)
        SamplerConfig sampler = temperature == null ? null
                : new SamplerConfig(SAMPLER_TOP_K, SAMPLER_TOP_P, temperature, (int) System.nanoTime());
        // Kotlin 쪽에서 null을 받지 않는 리스트·맵 인자에는 빈 값을 넘긴다. 생각 모드는 끈다
        ConversationConfig config = new ConversationConfig(null, Collections.emptyList(), Collections.emptyList(),
                sampler, true, null, Collections.emptyMap(), null, false, maxOutputTokens, new ThinkingConfig(false));
        long start = SystemClock.elapsedRealtime();
        try (Conversation conversation = engine.createConversation(config)) {
            running = conversation;
            if (cancelRequested) return null; // running을 넣기 전에 cancel()이 온 경우
            GeminiBackend.logRaw("온디바이스 요청 (" + device + ")", prompt);
            Message message = conversation.sendMessage(prompt);
            if (cancelRequested) return null;
            String text = THINK_BLOCK.matcher(textOf(message)).replaceAll("").trim();
            GeminiBackend.logRaw("온디바이스 응답 " + (SystemClock.elapsedRealtime() - start) + "ms", text);
            return text;
        } finally {
            running = null;
        }
    }

    /** 디버그 플래그로 응답을 일부러 늦춘다. cancel()이 오면 바로 멈춘다 */
    private void debugDelay() throws InterruptedException {
        long delay = OnDeviceDebugFlags.delayMs(appContext);
        if (delay <= 0) return;
        Log.d(TAG, "디버그 지연 " + delay + "ms");
        long end = SystemClock.elapsedRealtime() + delay;
        while (!cancelRequested && SystemClock.elapsedRealtime() < end) {
            Thread.sleep(50);
        }
    }

    @Override
    public void cancel() {
        if (!busy.get()) return;
        cancelRequested = true;
        Conversation conversation = running;
        if (conversation == null) return;
        try {
            conversation.cancelProcess();
            Log.i(TAG, "온디바이스 추론 중단 요청");
        } catch (Throwable t) {
            Log.w(TAG, "온디바이스 추론 중단 실패", t);
        }
    }

    private void load() {
        if (engine != null) {
            // 해제를 요청한 뒤 닫기 전에 다시 warmUp된 경우: 남아 있는 엔진을 그대로 쓴다
            synchronized (this) {
                if (state == State.LOADING) state = State.READY;
            }
            return;
        }
        String reason = unavailableReason();
        File model = reason == null ? ModelLocator.find(appContext) : null;
        missingModel = reason == null && model == null;
        if (missingModel) reason = "모델 파일 없음";
        if (reason != null) {
            Log.i(TAG, "온디바이스 사용 안 함: " + reason);
            state = State.UNAVAILABLE;
            return;
        }

        for (String which : deviceOrder()) {
            long start = SystemClock.elapsedRealtime();
            Engine e = null;
            try {
                Backend backend = which.equals("GPU") ? new Backend.GPU() : new Backend.CPU();
                e = new Engine(new EngineConfig(model.getPath(), backend, null, null,
                        MAX_NUM_TOKENS, null, appContext.getCacheDir().getPath()));
                e.initialize();
            } catch (Throwable t) {
                // 네이티브 라이브러리 로드 실패(UnsatisfiedLinkError)는 Exception이 아니라 Error라서 Throwable로 잡는다
                Log.w(TAG, which + " 초기화 실패 (" + (SystemClock.elapsedRealtime() - start) + "ms)", t);
                closeQuietly(e);
                continue;
            }
            Log.i(TAG, which + " 초기화 " + (SystemClock.elapsedRealtime() - start) + "ms: " + model.getName());
            synchronized (this) {
                if (state == State.LOADING) {
                    engine = e;
                    device = which;
                    state = State.READY;
                    return;
                }
            }
            // 로딩 중에 해제를 요청받았다
            closeQuietly(e);
            return;
        }
        state = State.FAILED;
    }

    /** executor 스레드에서만 부른다 */
    private void markFailed() {
        synchronized (this) {
            state = State.FAILED;
        }
        closeQuietly(engine);
        engine = null;
    }

    private void closeEngine() {
        synchronized (this) {
            // 해제를 요청한 뒤 다시 warmUp되었으면 닫지 않고 그대로 쓴다
            if (state == State.LOADING || state == State.READY) return;
        }
        if (engine == null) return;
        closeQuietly(engine);
        engine = null;
        Log.i(TAG, "엔진 해제");
    }

    /** 출시 빌드는 GPU만 쓴다 (실패하면 주사위는 결과표). 디버그 빌드는 플래그로 CPU도 쓸 수 있다 */
    private List<String> deviceOrder() {
        if (!BuildConfig.DEBUG) return Collections.singletonList("GPU");
        switch (OnDeviceDebugFlags.backend(appContext)) {
            case "cpu":
                return Collections.singletonList("CPU");
            case "auto":
                return Arrays.asList("GPU", "CPU");
            default:
                return Collections.singletonList("GPU");
        }
    }

    /** 로드를 시도하지 않을 이유. 괜찮으면 null */
    private String unavailableReason() {
        String unsupported = deviceUnsupportedReason(appContext);
        if (unsupported != null) return unsupported;

        long free = appContext.getCacheDir().getUsableSpace();
        if (free < MIN_FREE_CACHE) return "저장 공간 부족 (" + (free >> 20) + "MB)";
        return null;
    }

    /** 이 기기에서 모델을 쓰지 않을 이유(ABI, 램). 괜찮으면 null. ModelDownloader도 이걸 보고 받을지 정한다 */
    static String deviceUnsupportedReason(Context context) {
        List<String> abis = Arrays.asList(Build.SUPPORTED_64_BIT_ABIS);
        boolean abiOk = abis.contains("arm64-v8a") || (BuildConfig.DEBUG && abis.contains("x86_64"));
        if (!abiOk) return "지원하지 않는 ABI " + abis;

        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null && !OnDeviceDebugFlags.skipRamCheck(context)) {
            ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(info);
            if (am.isLowRamDevice() || info.totalMem < MIN_TOTAL_RAM) {
                return "램 기준 미달 (" + (info.totalMem >> 20) + "MB)";
            }
        }
        return null;
    }

    private static String textOf(Message message) {
        StringBuilder sb = new StringBuilder();
        for (Content content : message.getContents().getContents()) {
            if (content instanceof Content.Text) sb.append(((Content.Text) content).getText());
        }
        return sb.toString();
    }

    private static void closeQuietly(Engine e) {
        if (e == null) return;
        try {
            e.close();
        } catch (Throwable t) {
            Log.w(TAG, "엔진 닫기 실패", t);
        }
    }

    @Override
    public boolean isReady() {
        return state == State.READY && !busy.get();
    }

    @Override
    public String name() {
        return device.isEmpty() ? "OnDevice" : "OnDevice(" + device + ")";
    }
}
