package com.textdungeon.ai;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.ai.edge.litertlm.Backend;
import com.google.ai.edge.litertlm.BenchmarkInfo;
import com.google.ai.edge.litertlm.Content;
import com.google.ai.edge.litertlm.Conversation;
import com.google.ai.edge.litertlm.ConversationConfig;
import com.google.ai.edge.litertlm.Engine;
import com.google.ai.edge.litertlm.EngineConfig;
import com.google.ai.edge.litertlm.Message;
import com.google.ai.edge.litertlm.ThinkingConfig;
import com.textdungeon.ai.backend.GeminiBackend;
import com.textdungeon.ai.backend.LlmCallback;
import com.textdungeon.ai.backend.ResponseFormat;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.event.GameEvent;
import com.textdungeon.event.ShopEvent;
import com.textdungeon.model.Item;
import com.textdungeon.model.RewardStat;
import com.textdungeon.model.Stat;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 0-A 측정: 혼돈의 주사위 프롬프트를 온디바이스 모델(LiteRT-LM)과 클라우드(Gemini)에 똑같이 보내
 * 형식 성공률, 생각 모드, 응답 시간을 잰다. 디버그 빌드에만 들어가고 게임 화면과는 연결하지 않는다.
 *
 * 실행 (모델은 먼저 adb push로 files/models/에 넣어 둔다):
 *   adb shell am start -n com.lkj.textdungeon/com.textdungeon.ai.OnDeviceSpikeActivity
 *     --es model 파일명    models/ 안의 모델 (기본 gemma-4-E2B-it.litertlm)
 *     --es backend auto    auto = GPU 시도 후 실패하면 CPU / gpu / cpu
 *     --ez cloud true      클라우드 응답 시간도 잴지
 *     --ez nothink false   프롬프트 끝에 /no_think를 붙일지 (ThinkingConfig로 안 꺼질 때)
 *     --ei count 20
 *     --es key dice        클라우드 키: dice = GEMINI_API_KEY(주사위용) / gods = GEMINI_API_KEY_GODS(이벤트 생성용)
 * 결과: Logcat 태그 SPIKE, 파일 files/spike/spike_시각.md (adb pull로 가져온다)
 *
 * models/ 폴더는 앱이 만들어야 한다. adb shell mkdir로 만들면 주인이 셸이라 앱이 읽지 못한다.
 * 처음 한 번 이 Activity를 실행하면 폴더가 생기고 "모델 파일 없음"이 뜬다. 그다음 adb push 하고 다시 실행.
 */
public class OnDeviceSpikeActivity extends AppCompatActivity {
    private static final String TAG = "SPIKE";
    // 0-A에서 Qwen3-1.7B는 한국어 품질 미달로 탈락, Gemma 4 E2B로 결정
    private static final String DEFAULT_MODEL = com.textdungeon.ai.backend.ModelLocator.MODEL_FILE;
    private static final int MAX_OUTPUT_TOKENS = 200;
    // 지금 프롬프트(600자 안팎, 아이템 후보 5개)가 들어가고도 남는 크기. 실제 입력 토큰 수는 결과에 기록된다
    private static final int MAX_NUM_TOKENS = 4096;
    private static final long CLOUD_TIMEOUT_SEC = 60;
    private static final Pattern THINK_BLOCK = Pattern.compile("(?s)<think>.*?(</think>|$)");

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private TextView output;
    private Integer maxNumTokens = MAX_NUM_TOKENS;

    private static class Case {
        GameEvent event;
        int floor;
        Set<String> validIds;
        String prompt;
    }

    private static class Result {
        long ms = -1;
        boolean ok;
        String error = "";
        ChoiceDelta delta;
        String raw = "";
        boolean think;
        String channels = "";
        int prefillTokens = -1;
        int decodeTokens = -1;
        double decodeSpeed = -1;
        double ttft = -1;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        output = new TextView(this);
        output.setTextIsSelectable(true);
        output.setTextSize(11);
        output.setPadding(24, 96, 24, 96);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(output);
        setContentView(scroll);

        // 화면 회전 등으로 다시 만들어질 때 측정이 두 번 돌지 않게
        if (savedInstanceState != null) return;
        Intent intent = getIntent();
        String model = valueOr(intent.getStringExtra("model"), DEFAULT_MODEL);
        String backend = valueOr(intent.getStringExtra("backend"), "auto");
        boolean cloud = intent.getBooleanExtra("cloud", true);
        boolean noThink = intent.getBooleanExtra("nothink", false);
        int count = intent.getIntExtra("count", 20);
        String key = "gods".equals(intent.getStringExtra("key"))
                ? ApiKeyManager.getGeminiGodsKey() : ApiKeyManager.getGeminiKey();
        String probe = intent.getStringExtra("probe");
        // --ei maxtokens 0 이면 모델 기본값(null)을 쓴다
        int maxTokens = intent.getIntExtra("maxtokens", MAX_NUM_TOKENS);
        maxNumTokens = maxTokens > 0 ? maxTokens : null;
        int dice = intent.getIntExtra("dice", 0);
        boolean waitReady = intent.getBooleanExtra("wait", true);
        boolean release = intent.getBooleanExtra("release", false);
        new Thread(() -> {
            try {
                if (dice > 0) {
                    runDice(dice, waitReady, release);
                    return;
                }
                if (probe != null) {
                    probe(model, backend, probe);
                    return;
                }
                run(model, backend, cloud ? key : null, noThink, count);
            } catch (Throwable t) {
                Log.e(TAG, "측정 중단", t);
                show("측정 중단: " + t);
            }
        }, "spike").start();
    }

    private static String valueOr(String v, String fallback) {
        return v == null || v.isEmpty() ? fallback : v;
    }

    private void show(String line) {
        Log.i(TAG, line);
        mainHandler.post(() -> output.append(line + "\n"));
    }

    /** @param cloudKey 클라우드를 잴 때 쓸 키. null이면 클라우드는 건너뛴다 */
    private void run(String modelName, String backendMode, String cloudKey, boolean noThink, int count) throws Exception {
        DataControlTower dt = DataControlTower.getInstance(this);
        List<Case> cases = buildCases(dt, count);
        show("이벤트 " + cases.size() + "개, backend=" + backendMode
                + ", nothink=" + noThink + ", cloud=" + (cloudKey != null));

        // 1) 클라우드: 같은 프롬프트를 하나씩 순서대로 보내 응답 시간만 잰다
        List<Result> cloudResults = new ArrayList<>();
        if (cloudKey != null) {
            GeminiBackend cloud = new GeminiBackend(AiManager.GEMINI_MODEL, cloudKey);
            for (int i = 0; i < cases.size(); i++) {
                Result r = runCloud(cloud, cases.get(i));
                cloudResults.add(r);
                show("클라우드 " + (i + 1) + "/" + cases.size() + " " + r.ms + "ms " + (r.ok ? "OK" : "실패: " + r.error));
            }
        }

        // 2) 온디바이스 엔진 초기화 (GPU 먼저, 실패하면 CPU)
        File modelDir = new File(getExternalFilesDir(null), "models");
        //noinspection ResultOfMethodCallIgnored
        modelDir.mkdirs();
        File modelFile = new File(modelDir, modelName);
        StringBuilder initLog = new StringBuilder();
        long pssBefore = Debug.getPss();
        String usedBackend = null;
        long initMs = -1;
        Engine engine = null;
        if (!modelFile.exists()) {
            initLog.append("모델 파일 없음: ").append(modelFile.getPath());
        } else {
            File gpuMarker = new File(getFilesDir(), "spike_gpu_attempt");
            List<String> order = new ArrayList<>();
            if (backendMode.equals("gpu") || backendMode.equals("auto")) order.add("gpu");
            if (backendMode.equals("cpu") || backendMode.equals("auto")) order.add("cpu");
            if (backendMode.equals("auto") && gpuMarker.exists()) {
                // GPU 초기화가 예외 없이 프로세스째 죽으면 다음 실행에서 이 표시로 알 수 있다
                initLog.append("GPU: 이전 실행에서 초기화 중 프로세스가 종료됨 → 건너뜀. ");
                order.remove("gpu");
            }
            for (String which : order) {
                show(which.toUpperCase(Locale.ROOT) + " 엔진 초기화 시작...");
                long start = SystemClock.elapsedRealtime();
                try {
                    if (which.equals("gpu")) gpuMarker.createNewFile();
                    Backend backend = which.equals("gpu") ? new Backend.GPU() : new Backend.CPU();
                    Engine e = new Engine(new EngineConfig(modelFile.getPath(), backend, null, null,
                            maxNumTokens, null, getCacheDir().getPath()));
                    e.initialize();
                    initMs = SystemClock.elapsedRealtime() - start;
                    engine = e;
                    usedBackend = which;
                    initLog.append(which.toUpperCase(Locale.ROOT)).append(": 성공 ").append(initMs).append("ms. ");
                } catch (Throwable t) {
                    Log.e(TAG, which + " 초기화 실패", t);
                    initLog.append(which.toUpperCase(Locale.ROOT)).append(": 실패 ")
                            .append(SystemClock.elapsedRealtime() - start).append("ms (")
                            .append(t.getClass().getSimpleName()).append(": ").append(t.getMessage()).append("). ");
                } finally {
                    if (which.equals("gpu")) gpuMarker.delete();
                }
                if (engine != null) break;
            }
        }
        show("엔진: " + initLog);
        long pssLoaded = Debug.getPss();

        // 3) 온디바이스 생성
        List<Result> deviceResults = new ArrayList<>();
        if (engine != null) {
            for (int i = 0; i < cases.size(); i++) {
                Result r = runOnDevice(engine, cases.get(i), noThink);
                deviceResults.add(r);
                show("온디바이스 " + (i + 1) + "/" + cases.size() + " " + r.ms + "ms " + (r.ok ? "OK" : "실패: " + r.error)
                        + (r.think ? " <think> 남음" : "") + " 입력 " + r.prefillTokens + "토큰, 출력 " + r.decodeTokens + "토큰");
            }
        }
        long pssAfter = Debug.getPss();
        if (engine != null) engine.close();

        File report = writeReport(modelFile, backendMode, noThink, usedBackend, initLog.toString(), initMs,
                pssBefore, pssLoaded, pssAfter, cases, deviceResults, cloudResults);
        show("완료. 결과 파일: " + report.getPath());
    }

    /**
     * 진단용: 짧은 프롬프트 하나를 여러 설정으로 보내 원문을 Logcat에 남긴다.
     * --es probe 텍스트 ("dice"면 첫 번째 주사위 프롬프트를 쓴다)
     */
    private void probe(String modelName, String backendMode, String text) throws Exception {
        File modelFile = new File(new File(getExternalFilesDir(null), "models"), modelName);
        Backend backend = backendMode.equals("gpu") ? new Backend.GPU() : new Backend.CPU();
        if ("dice".equals(text)) {
            DataControlTower dt = DataControlTower.getInstance(this);
            text = buildCases(dt, 1).get(0).prompt;
        }
        try (Engine engine = new Engine(new EngineConfig(modelFile.getPath(), backend, null, null,
                maxNumTokens, null, getCacheDir().getPath()))) {
            engine.initialize();
            String[] labels = {"ThinkingConfig(false)", "ThinkingConfig 없음", "/no_think 붙임, ThinkingConfig 없음"};
            for (int i = 0; i < labels.length; i++) {
                ConversationConfig config = new ConversationConfig(null, Collections.emptyList(),
                        Collections.emptyList(), null, true, null, Collections.emptyMap(), null, false,
                        MAX_OUTPUT_TOKENS, i == 0 ? new ThinkingConfig(false) : null);
                long start = SystemClock.elapsedRealtime();
                try (Conversation conversation = engine.createConversation(config)) {
                    Message message = conversation.sendMessage(i == 2 ? text + " /no_think" : text);
                    show("[" + labels[i] + "] " + (SystemClock.elapsedRealtime() - start) + "ms 채널="
                            + message.getChannels() + "\n" + textOf(message));
                } catch (Throwable t) {
                    show("[" + labels[i] + "] 실패: " + t);
                }
            }
        }
        show("진단 완료");
    }

    /**
     * 1단계 확인: 게임과 같은 경로(AiManager → ChaosDice → FallbackBackend → 온디바이스/클라우드)로 주사위를 굴린다.
     *   --ei dice 3        굴릴 횟수
     *   --ez wait true     모델이 준비될 때까지 기다린 뒤 굴림 (false면 로딩 중에 바로 굴림)
     *   --ez release true  끝나면 모델을 내리고 메모리를 다시 잰다
     * 어느 백엔드가 응답했는지는 Logcat 태그 FallbackBackend. 디버그 플래그는 OnDeviceFlagsActivity로 바꾼다.
     */
    private void runDice(int count, boolean waitReady, boolean release) throws Exception {
        DataControlTower dt = DataControlTower.getInstance(this);
        AiManager ai = dt.getAiManager();
        long pssBefore = Debug.getPss();
        long start = SystemClock.elapsedRealtime();
        ai.warmUpOnDevice();
        if (waitReady) {
            while (isLoading(ai.getOnDeviceState()) && SystemClock.elapsedRealtime() - start < 5 * 60_000) {
                Thread.sleep(200);
            }
        }
        show("모델 상태: " + ai.getOnDeviceState() + " (" + (SystemClock.elapsedRealtime() - start) + "ms), PSS "
                + pssBefore / 1024 + "MB → " + Debug.getPss() / 1024 + "MB");

        List<Case> cases = buildCases(dt, count);
        int[] afterCallback = new int[cases.size()];
        for (int i = 0; i < cases.size(); i++) {
            Case c = cases.get(i);
            int before = c.event.getChoices().size();
            CountDownLatch latch = new CountDownLatch(1);
            String[] outcome = new String[1];
            long t0 = SystemClock.elapsedRealtime();
            // 보통 난이도(보상 2배), 경험치 특성 없음
            ai.requestChaosChoice(c.floor, statForFloor(c.floor), dt.getItemsForFloor(c.floor - 5, c.floor + 5),
                    c.event, 2, 1, new AiCallback() {
                        @Override
                        public void onSuccess(GameEvent updatedEvent) {
                            List<String> choices = updatedEvent.getChoices();
                            List<com.textdungeon.model.Reward> rewards = updatedEvent.getRewards();
                            outcome[0] = "성공 \"" + choices.get(choices.size() - 1) + "\" → "
                                    + rewards.get(rewards.size() - 1).getDescription();
                            latch.countDown();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            outcome[0] = "오류 " + errorMessage;
                            latch.countDown();
                        }
                    });
            boolean done = latch.await(AiTimeouts.DICE_TOTAL_MS + 10_000, TimeUnit.MILLISECONDS);
            afterCallback[i] = c.event.getChoices().size();
            show("주사위 " + (i + 1) + "/" + cases.size() + " " + (SystemClock.elapsedRealtime() - t0) + "ms "
                    + (done ? outcome[0] : "콜백 없음") + ", 선택지 " + before + "→" + afterCallback[i]
                    + ", 모델 " + modelStatus(ai));
        }

        // 시간 초과 뒤에 늦게 도착한 응답이 선택지를 더 붙이지 않았는지 확인
        Thread.sleep(10_000);
        for (int i = 0; i < cases.size(); i++) {
            int now = cases.get(i).event.getChoices().size();
            if (now != afterCallback[i]) show("주사위 " + (i + 1) + ": 콜백 뒤에 선택지가 더 붙음 " + afterCallback[i] + "→" + now);
        }
        show("10초 뒤 모델 " + modelStatus(ai) + ", PSS " + Debug.getPss() / 1024 + "MB");

        if (release) {
            ai.releaseOnDevice();
            Thread.sleep(3_000);
            show("해제 뒤 모델 " + ai.getOnDeviceState() + ", PSS " + Debug.getPss() / 1024 + "MB");
        }
        show("완료.");
    }

    /** 상태와, READY인데 앞 요청을 처리 중이라 받을 수 없는지 */
    private static String modelStatus(AiManager ai) {
        com.textdungeon.ai.backend.OnDeviceBackend.State state = ai.getOnDeviceState();
        boolean busy = state == com.textdungeon.ai.backend.OnDeviceBackend.State.READY && !ai.isOnDeviceReady();
        return state + (busy ? "(추론 중)" : "");
    }

    private static boolean isLoading(com.textdungeon.ai.backend.OnDeviceBackend.State state) {
        return state == com.textdungeon.ai.backend.OnDeviceBackend.State.NOT_LOADED
                || state == com.textdungeon.ai.backend.OnDeviceBackend.State.LOADING;
    }

    /**
     * 실제 이벤트 데이터에서 주사위를 쓸 수 있는 이벤트를 층 순서로 고르게 뽑는다.
     * 아이템 후보는 게임(EventActivity)과 같이 그 층대 아이템 중 ChaosDice.pickItems로 고른다.
     */
    private static List<Case> buildCases(DataControlTower dt, int count) {
        List<GameEvent> pool = new ArrayList<>();
        for (GameEvent e : dt.getEventManager().getAll()) {
            if (e instanceof ShopEvent || !e.canAppendChoice()) continue;
            if (e.getChoices() == null || e.getChoices().isEmpty()) continue;
            pool.add(e);
        }
        pool.sort(Comparator.comparingInt(GameEvent::getMinFloor).thenComparing(GameEvent::getId));
        int n = Math.min(count, pool.size());
        Random rnd = new Random(42); // 매번 같은 이벤트·층으로 비교하기 위해 고정
        List<Case> cases = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Case c = new Case();
            c.event = pool.get(i * pool.size() / n);
            int span = Math.max(1, c.event.getMaxFloor() - c.event.getMinFloor() + 1);
            c.floor = Math.max(1, c.event.getMinFloor() + rnd.nextInt(span));
            List<Item> items = ChaosDice.pickItems(dt.getItemsForFloor(c.floor - 5, c.floor + 5), rnd);
            c.validIds = new HashSet<>();
            for (Item item : items) c.validIds.add(item.getId());
            c.prompt = ChaosDice.buildPrompt(c.floor, statForFloor(c.floor), items, c.event);
            cases.add(c);
        }
        return cases;
    }

    /** 실제 판의 성장 곡선을 흉내만 낸 값. 프롬프트에 들어가는 수치가 층에 맞게 커지면 된다 */
    private static Stat statForFloor(int floor) {
        Stat s = new Stat(11 + floor / 2, 6 + floor / 3, 11 + floor / 2, 2 + floor / 4);
        s.updateBattleStat(floor / 3);
        s.setHp(s.getMaxHp() * 3 / 4);
        s.addGold(floor * 40);
        return s;
    }

    private static Result runCloud(GeminiBackend cloud, Case c) throws InterruptedException {
        Result r = new Result();
        CountDownLatch latch = new CountDownLatch(1);
        String[] text = new String[1];
        String[] error = new String[1];
        long start = SystemClock.elapsedRealtime();
        cloud.generate(c.prompt, ResponseFormat.TEXT, new LlmCallback() {
            @Override
            public void onResult(String t) {
                text[0] = t;
                latch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                error[0] = errorMessage;
                latch.countDown();
            }
        });
        boolean done = latch.await(CLOUD_TIMEOUT_SEC, TimeUnit.SECONDS);
        r.ms = SystemClock.elapsedRealtime() - start;
        if (!done) {
            r.error = "시간 초과";
        } else if (error[0] != null) {
            r.error = error[0];
        } else {
            parseInto(r, text[0], c);
        }
        return r;
    }

    private static Result runOnDevice(Engine engine, Case c, boolean noThink) {
        Result r = new Result();
        // Kotlin 쪽에서 null을 받지 않는 리스트·맵 인자에는 빈 값을 넘긴다
        ConversationConfig config = new ConversationConfig(null, Collections.emptyList(), Collections.emptyList(),
                null, true, null, Collections.emptyMap(), null, false, MAX_OUTPUT_TOKENS, new ThinkingConfig(false));
        long start = SystemClock.elapsedRealtime();
        try (Conversation conversation = engine.createConversation(config)) {
            Message message = conversation.sendMessage(noThink ? c.prompt + "\n/no_think" : c.prompt);
            r.ms = SystemClock.elapsedRealtime() - start;
            Map<String, String> channels = message.getChannels();
            if (channels != null && !channels.isEmpty()) r.channels = channels.toString();
            try {
                BenchmarkInfo b = conversation.getBenchmarkInfo();
                r.prefillTokens = b.getLastPrefillTokenCount();
                r.decodeTokens = b.getLastDecodeTokenCount();
                r.decodeSpeed = b.getLastDecodeTokensPerSecond();
                r.ttft = b.getTimeToFirstTokenInSecond();
            } catch (Throwable ignore) {
                // 벤치마크 정보가 꺼져 있으면 시간만 기록
            }
            parseInto(r, textOf(message), c);
        } catch (Throwable t) {
            r.ms = SystemClock.elapsedRealtime() - start;
            r.error = t.getClass().getSimpleName() + ": " + t.getMessage();
            Log.e(TAG, "온디바이스 생성 실패", t);
        }
        return r;
    }

    private static String textOf(Message message) {
        StringBuilder sb = new StringBuilder();
        for (Content content : message.getContents().getContents()) {
            if (content instanceof Content.Text) sb.append(((Content.Text) content).getText());
        }
        return sb.toString();
    }

    /** 1단계에서 할 것처럼 남은 <think> 블록을 지운 뒤 게임과 같은 기준(ChaosDice.parseResponse)으로 읽는다 */
    private static void parseInto(Result r, String raw, Case c) {
        r.raw = raw == null ? "" : raw;
        r.think = r.raw.contains("<think>");
        String cleaned = THINK_BLOCK.matcher(r.raw).replaceAll("").trim();
        try {
            r.delta = ChaosDice.parseResponse(cleaned, c.validIds, c.event).withClampedStats(c.floor);
            r.ok = true;
        } catch (ChoiceLineParser.ParseException e) {
            r.error = e.getMessage();
        }
    }

    // ───────────── 결과 보고서 ─────────────

    private File writeReport(File modelFile, String backendMode, boolean noThink, String usedBackend, String initLog,
                             long initMs, long pssBefore, long pssLoaded, long pssAfter, List<Case> cases,
                             List<Result> device, List<Result> cloud) throws Exception {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mem = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mem);
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(new Date());

        StringBuilder md = new StringBuilder();
        md.append("# 0-A 측정 결과 ").append(stamp).append("\n\n");
        md.append("- 기기: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
                .append(", Android ").append(Build.VERSION.RELEASE).append(", ABI ").append(Build.SUPPORTED_ABIS[0])
                .append(", RAM ").append(mem.totalMem / (1024 * 1024)).append("MB\n");
        md.append("- 모델: ").append(modelFile.getName()).append(" (").append(modelFile.length() / (1024 * 1024)).append("MiB)\n");
        md.append("- 옵션: backend=").append(backendMode).append(", nothink=").append(noThink)
                .append(", 최대 출력 ").append(MAX_OUTPUT_TOKENS).append("토큰, ThinkingConfig(false)\n\n");

        md.append("## 요약\n\n| 항목 | 값 |\n| --- | --- |\n");
        md.append("| 엔진 초기화 | ").append(initLog.trim()).append(" |\n");
        md.append("| 실제 장치 | ").append(usedBackend == null ? "없음" : usedBackend.toUpperCase(Locale.ROOT)).append(" |\n");
        md.append("| 형식 성공률 (온디바이스) | ").append(countOk(device)).append('/').append(device.size()).append(" |\n");
        md.append("| `<think>`가 남은 응답 | ").append(countThink(device)).append('/').append(device.size()).append(" |\n");
        md.append("| 생각 채널에 내용이 있는 응답 | ").append(countChannels(device)).append('/').append(device.size()).append(" |\n");
        md.append("| 출력이 ").append(MAX_OUTPUT_TOKENS).append("토큰에서 잘린 응답 | ").append(countCut(device)).append('/').append(device.size()).append(" |\n");
        md.append("| 온디바이스 응답 시간 | ").append(timeSummary(device)).append(" |\n");
        md.append("| 온디바이스 입력 토큰 | ").append(tokenSummary(device)).append(" |\n");
        md.append("| 프롬프트 길이 | ").append(promptLengths(cases)).append(" |\n");
        md.append("| 형식 성공률 (클라우드) | ").append(countOk(cloud)).append('/').append(cloud.size()).append(" |\n");
        md.append("| 클라우드 응답 시간 | ").append(timeSummary(cloud)).append(" |\n");
        md.append("| 앱 메모리(PSS) | 로드 전 ").append(pssBefore / 1024).append("MB → 로드 후 ").append(pssLoaded / 1024)
                .append("MB → 생성 후 ").append(pssAfter / 1024).append("MB |\n\n");

        md.append("## 건별 결과\n\n| # | 이벤트 | 층 | 온디바이스 | 형식 | 클라우드 | 형식 |\n| --- | --- | --- | --- | --- | --- | --- |\n");
        for (int i = 0; i < cases.size(); i++) {
            Case c = cases.get(i);
            Result d = i < device.size() ? device.get(i) : null;
            Result k = i < cloud.size() ? cloud.get(i) : null;
            md.append("| ").append(i + 1).append(" | ").append(c.event.getName()).append(" (").append(c.event.getId())
                    .append(") | ").append(c.floor).append(" | ").append(d == null ? "-" : d.ms + "ms")
                    .append(" | ").append(d == null ? "-" : d.ok ? "OK" : "실패: " + d.error)
                    .append(" | ").append(k == null ? "-" : k.ms + "ms")
                    .append(" | ").append(k == null ? "-" : k.ok ? "OK" : "실패: " + k.error).append(" |\n");
        }

        md.append("\n## 온디바이스 응답 (한국어 품질 판단용)\n");
        appendDetails(md, cases, device);
        md.append("\n## 클라우드 응답 (비교용)\n");
        appendDetails(md, cases, cloud);
        md.append("\n## 첫 번째 프롬프트 전문\n\n```\n").append(cases.isEmpty() ? "" : cases.get(0).prompt).append("\n```\n");

        File dir = new File(getExternalFilesDir(null), "spike");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        File file = new File(dir, "spike_" + stamp + ".md");
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(md.toString().getBytes(StandardCharsets.UTF_8));
        }
        return file;
    }

    private static void appendDetails(StringBuilder md, List<Case> cases, List<Result> results) {
        for (int i = 0; i < results.size(); i++) {
            Case c = cases.get(i);
            Result r = results.get(i);
            md.append("\n### ").append(i + 1).append(". ").append(c.event.getName())
                    .append(" (").append(c.floor).append("층, ").append(r.ms).append("ms)\n\n");
            md.append("- 상황: ").append(c.event.getDescription()).append('\n');
            md.append("- 기존 선택지: ").append(String.join(" / ", c.event.getChoices())).append('\n');
            if (r.ok) {
                md.append("- **선택지**: ").append(r.delta.getChoiceText()).append('\n');
                md.append("- **보상설명**: ").append(r.delta.getRewardDescription()).append('\n');
                md.append("- 아이템: ").append(r.delta.getItemId() == null ? "없음" : r.delta.getItemId()).append('\n');
                md.append("- 스탯(층 상한 적용 후): ").append(statsText(r.delta.getStats())).append('\n');
            } else {
                md.append("- **실패**: ").append(r.error).append('\n');
            }
            if (!r.channels.isEmpty()) md.append("- 채널: ").append(r.channels.replace('\n', ' ')).append('\n');
            md.append("\n```\n").append(r.raw.trim()).append("\n```\n");
        }
    }

    private static String statsText(List<RewardStat> stats) {
        if (stats == null || stats.isEmpty()) return "없음";
        StringBuilder sb = new StringBuilder();
        for (RewardStat s : stats) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(s.getStatType()).append(' ').append(s.getValue() > 0 ? "+" : "").append(s.getValue());
        }
        return sb.toString();
    }

    private static int countOk(List<Result> rs) {
        int n = 0;
        for (Result r : rs) if (r.ok) n++;
        return n;
    }

    private static int countThink(List<Result> rs) {
        int n = 0;
        for (Result r : rs) if (r.think) n++;
        return n;
    }

    private static int countChannels(List<Result> rs) {
        int n = 0;
        for (Result r : rs) if (!r.channels.isEmpty()) n++;
        return n;
    }

    private static int countCut(List<Result> rs) {
        int n = 0;
        for (Result r : rs) if (r.decodeTokens >= MAX_OUTPUT_TOKENS) n++;
        return n;
    }

    /** 중앙값 / 느린 쪽 10%(90번째 백분위, 최근접 순위) / 최대 */
    private static String timeSummary(List<Result> rs) {
        List<Long> ms = new ArrayList<>();
        for (Result r : rs) if (r.ms >= 0) ms.add(r.ms);
        if (ms.isEmpty()) return "-";
        Collections.sort(ms);
        long p50 = ms.get((int) Math.ceil(0.5 * ms.size()) - 1);
        long p90 = ms.get((int) Math.ceil(0.9 * ms.size()) - 1);
        return String.format(Locale.ROOT, "중앙값 %.1f초 / 느린 쪽 10%% %.1f초 / 최대 %.1f초",
                p50 / 1000.0, p90 / 1000.0, ms.get(ms.size() - 1) / 1000.0);
    }

    private static String promptLengths(List<Case> cases) {
        int min = Integer.MAX_VALUE, max = 0;
        for (Case c : cases) {
            min = Math.min(min, c.prompt.length());
            max = Math.max(max, c.prompt.length());
        }
        return cases.isEmpty() ? "-" : min + "~" + max + "자";
    }

    private static String tokenSummary(List<Result> rs) {
        int min = Integer.MAX_VALUE, max = -1;
        double speed = 0;
        int speedCount = 0;
        for (Result r : rs) {
            if (r.prefillTokens < 0) continue;
            min = Math.min(min, r.prefillTokens);
            max = Math.max(max, r.prefillTokens);
            if (r.decodeSpeed > 0) {
                speed += r.decodeSpeed;
                speedCount++;
            }
        }
        if (max < 0) return "기록 없음";
        return min + "~" + max + "토큰"
                + (speedCount > 0 ? String.format(Locale.ROOT, ", 평균 생성 속도 %.1f토큰/초", speed / speedCount) : "");
    }
}
