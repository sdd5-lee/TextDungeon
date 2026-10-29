package com.textdungeon.ai;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.textdungeon.ai.backend.GeminiBackend;
import com.textdungeon.ai.backend.LlmBackend;
import com.textdungeon.ai.backend.LlmCallback;
import com.textdungeon.ai.backend.OnDeviceBackend;
import com.textdungeon.ai.backend.ResponseFormat;
import com.textdungeon.data.DataControlTower;
import com.textdungeon.event.BattleEvent;
import com.textdungeon.event.GameEvent;
import com.textdungeon.model.Item;
import com.textdungeon.model.Monster;
import com.textdungeon.model.Reward;
import com.textdungeon.model.RewardStat;
import com.textdungeon.model.Stat;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * AI 이벤트 생성 비교 측정: JSON 방식(EventGenerator, 지금 게임)과 틀 방식(TemplateEventGenerator)을
 * 온디바이스 모델과 클라우드(Gemini)에 같은 상황으로 보내 속도, 성공률, 문장을 비교한다. 디버그 빌드 전용.
 *
 * 실행 (모델은 files/models/에 있어야 한다):
 *   adb shell am start -S -n com.lkj.textdungeon/com.textdungeon.ai.EventSpikeActivity
 *     --es targets device,cloud   잴 곳 (순서대로)
 *     --es methods json,template  잴 방식
 *     --ei count 20               신 5명 × 층 1, 11, 21, 41 중 앞에서부터
 * 결과: Logcat 태그 SPIKE ("완료"가 찍히면 끝), 파일 files/spike/event_시각.md
 */
public class EventSpikeActivity extends AppCompatActivity {
    private static final String TAG = "SPIKE";
    private static final int[] FLOORS = {1, 11, 21, 41};
    private static final long DEVICE_TIMEOUT_SEC = 180;
    private static final long CLOUD_TIMEOUT_SEC = 150;
    private static final long LOAD_TIMEOUT_MS = 5 * 60_000;
    // 보통 난이도(보상 2배), 경험치 특성 없음
    private static final int REWARD_MULTIPLIER = 2;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private TextView output;

    private static class Case {
        AiType god;
        int floor;
        TemplateEventGenerator.Plan plan;
        Set<String> deepIds;
        Set<String> monsterIds;
    }

    private static class Result {
        long ms;
        boolean ok;
        String error = "";
        GameEvent event;
        List<String> problems = new ArrayList<>();
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
        if (savedInstanceState != null) return;

        List<String> targets = Arrays.asList(valueOr(getIntent().getStringExtra("targets"), "device,cloud").split(","));
        List<String> methods = Arrays.asList(valueOr(getIntent().getStringExtra("methods"), "json,template").split(","));
        int count = getIntent().getIntExtra("count", 20);
        boolean repair = "repair".equals(getIntent().getStringExtra("mode"));
        new Thread(() -> {
            try {
                if (repair) runRepairCompare();
                else run(targets, methods, count);
            } catch (Throwable t) {
                Log.e(TAG, "측정 중단", t);
                show("측정 중단: " + t);
            }
        }, "event-spike").start();
    }

    private static String valueOr(String v, String fallback) {
        return v == null || v.isEmpty() ? fallback : v;
    }

    private void show(String line) {
        Log.i(TAG, line);
        mainHandler.post(() -> output.append(line + "\n"));
    }

    private void run(List<String> targets, List<String> methods, int count) throws Exception {
        DataControlTower dt = DataControlTower.getInstance(this);
        List<Case> cases = buildCases(dt, count);
        show("이벤트 " + cases.size() + "개, 대상 " + targets + ", 방식 " + methods);

        // 열 이름 "device/json" 같은 순서를 지키려고 LinkedHashMap
        Map<String, List<Result>> results = new LinkedHashMap<>();
        String deviceName = "";
        for (String target : targets) {
            LlmBackend backend;
            OnDeviceBackend device = null;
            if ("device".equals(target)) {
                device = loadDevice();
                if (device.getState() != OnDeviceBackend.State.READY) continue;
                deviceName = device.name();
                backend = device;
            } else {
                backend = new GeminiBackend(this, AiManager.GEMINI_MODEL, ApiKeyManager.getGeminiGodsKey());
            }
            long timeout = device != null ? DEVICE_TIMEOUT_SEC : CLOUD_TIMEOUT_SEC;
            for (String method : methods) {
                String column = target + "/" + method;
                List<Result> list = new ArrayList<>();
                for (int i = 0; i < cases.size(); i++) {
                    Result r = "json".equals(method)
                            ? runJson(backend, dt, cases.get(i), timeout)
                            : runTemplate(backend, cases.get(i), timeout);
                    check(cases.get(i), r, method);
                    list.add(r);
                    show(column + " " + (i + 1) + "/" + cases.size() + " " + r.ms + "ms "
                            + (r.ok ? "OK " + r.event.getName() + (r.problems.isEmpty() ? "" : " 문제 " + r.problems) : "실패: " + r.error));
                }
                results.put(column, list);
            }
            if (device != null) {
                device.release();
                Thread.sleep(3000); // 해제는 전용 스레드에서 차례로 처리된다
            }
        }
        File file = writeReport(cases, results, deviceName);
        show("완료. 결과 파일: " + file.getAbsolutePath());
    }

    private OnDeviceBackend loadDevice() throws InterruptedException {
        OnDeviceBackend device = new OnDeviceBackend(getApplicationContext());
        long start = SystemClock.elapsedRealtime();
        device.warmUp();
        while (device.getState() != OnDeviceBackend.State.READY
                && device.getState() != OnDeviceBackend.State.FAILED
                && device.getState() != OnDeviceBackend.State.UNAVAILABLE
                && SystemClock.elapsedRealtime() - start < LOAD_TIMEOUT_MS) {
            Thread.sleep(200);
        }
        show("온디바이스 " + device.getState() + " (" + (SystemClock.elapsedRealtime() - start) + "ms, " + device.name() + ")");
        return device;
    }

    // ── 고쳐 쓰기 대 다시 만들기 ─────────────────

    private static class RepairResult {
        long ms;
        boolean ok;
        int calls;
        String error = "";
        GameEvent event;
        List<String> changes = new ArrayList<>();
    }

    /**
     * 앞 측정에서 실패한 응답을 깨진 줄만 고쳐 쓰는 것과, 같은 뼈대로 이벤트를 다시 만드는 것을 번갈아 잰다.
     * 입력: files/spike/repair_input.txt. 한 줄에 "경우 번호<탭>원문", 원문의 줄바꿈은 " / " (보고서의 실패 원문 그대로).
     * 경우 번호는 같은 코드의 측정 보고서 번호다 (시드가 같아 뼈대가 같다)
     *   adb shell am start -S -n com.lkj.textdungeon/com.textdungeon.ai.EventSpikeActivity --es mode repair
     */
    private void runRepairCompare() throws Exception {
        DataControlTower dt = DataControlTower.getInstance(this);
        List<Case> cases = buildCases(dt, FLOORS.length * AiType.values().length);
        File input = new File(getExternalFilesDir(null), "spike/repair_input.txt");
        List<String[]> entries = new ArrayList<>();
        for (String line : new String(java.nio.file.Files.readAllBytes(input.toPath()), StandardCharsets.UTF_8).split("\n")) {
            String[] parts = line.split("\t", 2);
            if (parts.length == 2) entries.add(parts);
        }
        show("실패 응답 " + entries.size() + "개로 고쳐 쓰기 대 다시 만들기");
        OnDeviceBackend device = loadDevice();
        if (device.getState() != OnDeviceBackend.State.READY) return;

        List<Case> picked = new ArrayList<>();
        List<String> raws = new ArrayList<>();
        List<RepairResult> repairs = new ArrayList<>();
        List<Result> regens = new ArrayList<>();
        for (String[] e : entries) {
            Case c = cases.get(Integer.parseInt(e[0].trim()) - 1);
            String raw = e[1].replace(" / ", "\n");
            RepairResult rr = runRepair(device, c, raw);
            show("고쳐 쓰기 " + e[0] + " " + rr.ms + "ms " + (rr.ok ? "OK" : "실패: " + rr.error) + " " + rr.changes);
            Result rg = runTemplate(device, c, DEVICE_TIMEOUT_SEC);
            show("다시 만들기 " + e[0] + " " + rg.ms + "ms " + (rg.ok ? "OK " + rg.event.getName() : "실패: " + rg.error));
            picked.add(c);
            raws.add(e[0].trim());
            repairs.add(rr);
            regens.add(rg);
        }
        device.release();
        File file = writeRepairReport(picked, raws, repairs, regens, device.name());
        show("완료. 결과 파일: " + file.getAbsolutePath());
    }

    private RepairResult runRepair(LlmBackend backend, Case c, String raw) throws InterruptedException {
        RepairResult r = new RepairResult();
        long start = SystemClock.elapsedRealtime();
        Map<String, String> lines = TemplateEventGenerator.extractLines(raw);
        List<TemplateEventGenerator.Broken> broken = TemplateEventGenerator.brokenLines(lines);
        if (broken == null || broken.isEmpty()) {
            r.error = broken == null ? "한 줄로 못 고치는 문제" : "깨진 줄 없음";
        } else {
            for (TemplateEventGenerator.Broken b : broken) {
                String[] error = {""};
                String text = ask(backend, TemplateEventGenerator.repairPrompt(lines, b), error);
                r.calls++;
                if (text == null) {
                    r.error = error[0];
                    break;
                }
                String fixed = TemplateEventGenerator.repairedValue(text, b.key);
                r.changes.add(b.key + ": " + b.value + " → " + fixed);
                lines.put(b.key, fixed);
            }
            if (r.error.isEmpty()) {
                try {
                    r.event = new TemplateEventGenerator(backend).toEvent(TemplateEventGenerator.joinLines(lines), c.plan,
                            REWARD_MULTIPLIER, 1);
                    r.ok = true;
                } catch (Exception e) {
                    r.error = e.getMessage();
                }
            }
        }
        r.ms = SystemClock.elapsedRealtime() - start;
        return r;
    }

    /** 요청 하나를 보내고 답을 기다린다. 실패하면 null이고 이유는 error[0] */
    private static String ask(LlmBackend backend, String prompt, String[] error) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        String[] text = new String[1];
        backend.generate(prompt, ResponseFormat.TEXT, TemplateEventGenerator.REPAIR_MAX_OUTPUT_TOKENS,
                TemplateEventGenerator.TEMPERATURE, new LlmCallback() {
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
        if (!latch.await(DEVICE_TIMEOUT_SEC, TimeUnit.SECONDS)) error[0] = "시간 초과";
        return text[0];
    }

    private File writeRepairReport(List<Case> cases, List<String> numbers, List<RepairResult> repairs, List<Result> regens,
                                   String deviceName) throws Exception {
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        StringBuilder md = new StringBuilder("# 고쳐 쓰기 대 다시 만들기 ").append(stamp).append("\n\n")
                .append("- 기기: ").append(android.os.Build.MANUFACTURER).append(' ').append(android.os.Build.MODEL)
                .append(", ").append(deviceName).append("\n")
                .append("- 앞 측정에서 실패한 응답마다 깨진 줄만 고쳐 쓰기와 같은 뼈대로 다시 만들기를 번갈아 잰다\n\n")
                .append("| 방법 | 성공 | 중앙값 | 평균 | 최대 |\n| --- | --- | --- | --- | --- |\n");
        List<Long> repairTimes = new ArrayList<>();
        List<Long> regenTimes = new ArrayList<>();
        int repairOk = 0, regenOk = 0;
        for (int i = 0; i < cases.size(); i++) {
            repairTimes.add(repairs.get(i).ms);
            regenTimes.add(regens.get(i).ms);
            if (repairs.get(i).ok) repairOk++;
            if (regens.get(i).ok) regenOk++;
        }
        md.append(timeRow("고쳐 쓰기", repairOk, repairTimes)).append(timeRow("다시 만들기", regenOk, regenTimes));
        md.append("\n## 경우별\n");
        for (int i = 0; i < cases.size(); i++) {
            Case c = cases.get(i);
            RepairResult rr = repairs.get(i);
            Result rg = regens.get(i);
            md.append("\n### ").append(numbers.get(i)).append(". ").append(c.god.getGodName()).append(' ')
                    .append(c.floor).append("층\n")
                    .append("- 고쳐 쓰기 (").append(sec(rr.ms)).append(", 요청 ").append(rr.calls).append("번) ")
                    .append(rr.ok ? "성공" : "실패: " + rr.error).append("\n");
            for (String change : rr.changes) md.append("  - ").append(change).append("\n");
            md.append("- 다시 만들기 (").append(sec(rg.ms)).append(") ")
                    .append(rg.ok ? "성공: " + rg.event.getName() : "실패: " + rg.error).append("\n");
        }
        File dir = new File(getExternalFilesDir(null), "spike");
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("폴더를 못 만듦: " + dir);
        File file = new File(dir, "repair_" + stamp + ".md");
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(md.toString().getBytes(StandardCharsets.UTF_8));
        }
        return file;
    }

    private static String timeRow(String label, int ok, List<Long> times) {
        List<Long> sorted = new ArrayList<>(times);
        Collections.sort(sorted);
        long sum = 0;
        for (long t : sorted) sum += t;
        return "| " + label + " | " + ok + "/" + sorted.size() + " | " + sec(sorted.get(sorted.size() / 2)) + " | "
                + sec(sum / sorted.size()) + " | " + sec(sorted.get(sorted.size() - 1)) + " |\n";
    }

    private static List<Case> buildCases(DataControlTower dt, int count) {
        List<Case> cases = new ArrayList<>();
        Set<String> deepIds = new HashSet<>();
        for (Item item : dt.getDeepFloorGear()) deepIds.add(item.getId());
        for (int floor : FLOORS) {
            for (AiType god : AiType.values()) {
                if (cases.size() >= count) return cases;
                Case c = new Case();
                c.god = god;
                c.floor = floor;
                List<Monster> monsters = dt.getMonstersForFloor(floor, floor + 10);
                c.monsterIds = new HashSet<>();
                for (Monster m : monsters) c.monsterIds.add(m.getId());
                c.deepIds = deepIds;
                // 두 대상이 같은 뼈대를 받도록 경우마다 시드를 고정한다
                Random rnd = new Random(cases.size() + 1);
                GodForms.Form form = GodForms.pick(god, rnd);
                List<Item> items = TemplateEventGenerator.itemsFor(form.grade, dt.getItemsForFloor(floor - 5, floor + 5),
                        dt.getUpperGear(floor), dt.getDeepFloorGear());
                c.plan = TemplateEventGenerator.plan(god, floor, form, items, monsters, rnd);
                cases.add(c);
            }
        }
        return cases;
    }

    /** 지금 게임과 같은 입력 (DifficultyActivity.generateWithRetry) */
    private static Result runJson(LlmBackend backend, DataControlTower dt, Case c, long timeoutSec) throws InterruptedException {
        List<Item> items = c.god == AiType.TREASURE ? dt.getDeepFloorGear() : dt.getItemManager().getAll();
        String type = c.god == AiType.STRUGGLE ? "battle" : "normal";
        Result r = new Result();
        CountDownLatch latch = new CountDownLatch(1);
        long start = SystemClock.elapsedRealtime();
        new EventGenerator(backend).generate(c.floor, statForFloor(c.floor), items,
                dt.getMonstersForFloor(c.floor, c.floor + 10), type, c.god, callback(r, latch));
        await(r, latch, start, timeoutSec);
        return r;
    }

    private Result runTemplate(LlmBackend backend, Case c, long timeoutSec) throws InterruptedException {
        Result r = new Result();
        CountDownLatch latch = new CountDownLatch(1);
        long start = SystemClock.elapsedRealtime();
        TemplateEventGenerator parser = new TemplateEventGenerator(backend);
        AiCallback cb = callback(r, latch);
        backend.generate(TemplateEventGenerator.prompt(c.plan), ResponseFormat.TEXT,
                TemplateEventGenerator.MAX_OUTPUT_TOKENS, TemplateEventGenerator.TEMPERATURE, new LlmCallback() {
                    @Override
                    public void onResult(String text) {
                        try {
                            cb.onSuccess(parser.toEvent(text, c.plan, REWARD_MULTIPLIER, 1));
                        } catch (Exception e) {
                            cb.onError(e.getMessage() + " | 원문: " + text.replace("\n", " / "));
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        cb.onError(errorMessage);
                    }
                });
        await(r, latch, start, timeoutSec);
        return r;
    }

    private static AiCallback callback(Result r, CountDownLatch latch) {
        return new AiCallback() {
            @Override
            public void onSuccess(GameEvent event) {
                r.ok = true;
                r.event = event;
                latch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                r.error = errorMessage;
                latch.countDown();
            }
        };
    }

    private static void await(Result r, CountDownLatch latch, long start, long timeoutSec) throws InterruptedException {
        if (!latch.await(timeoutSec, TimeUnit.SECONDS)) r.error = timeoutSec + "초 안에 응답 없음";
        r.ms = SystemClock.elapsedRealtime() - start;
    }

    /** 게임에 넣었을 때 문제가 되는 점 (성공한 이벤트만) */
    private static void check(Case c, Result r, String method) {
        if (!r.ok) return;
        GameEvent e = r.event;
        List<String> choices = e.getChoices();
        List<Reward> rewards = e.getRewards();
        if (choices == null || choices.size() != 2) r.problems.add("선택지 " + (choices == null ? 0 : choices.size()) + "개");
        if (e.getName() == null || e.getName().length() > TemplateEventGenerator.MAX_NAME_LENGTH) r.problems.add("이름 길이");
        if (choices != null) {
            for (String ch : choices) {
                if (ch.length() > TemplateEventGenerator.MAX_CHOICE_LENGTH) r.problems.add("긴 선택지");
            }
        }
        if (c.god == AiType.STRUGGLE) {
            if (!(e instanceof BattleEvent)) r.problems.add("전투가 아님");
            else if (!c.monsterIds.contains(((BattleEvent) e).getEnemyId())) r.problems.add("층에 없는 몬스터");
        }
        // 틀 방식의 보물은 형상 등급에 맞는 후보에서 코드가 고르므로, 심층 장비인지는 JSON 방식만 본다
        if ("json".equals(method) && c.god == AiType.TREASURE && rewards != null && rewards.size() >= 2) {
            String a = rewards.get(0).getItemId();
            String b = rewards.get(1).getItemId();
            if (a == null || b == null || a.equals(b) || !c.deepIds.contains(a) || !c.deepIds.contains(b)) {
                r.problems.add("보물 둘이 아님");
            }
        }
        // 플레이어는 형상 이름으로 보상 세기를 짐작하므로 이름이나 상황에 그대로 나와야 한다
        if ("template".equals(method)) {
            String form = c.plan.form.name.replace(" ", "");
            if (!(e.getName() + e.getDescription()).replace(" ", "").contains(form)) r.problems.add("형상이 글에 없음");
        }
    }

    private static Stat statForFloor(int floor) {
        Stat s = new Stat(11 + floor / 2, 6 + floor / 3, 11 + floor / 2, 2 + floor / 4);
        s.updateBattleStat(floor / 3);
        s.setHp(s.getMaxHp() * 3 / 4);
        s.addGold(floor * 40);
        return s;
    }

    // ── 보고서 ───────────────────────────────────

    private File writeReport(List<Case> cases, Map<String, List<Result>> results, String deviceName) throws Exception {
        StringBuilder md = new StringBuilder();
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        md.append("# 이벤트 생성 비교 ").append(stamp).append("\n\n");
        md.append("- 기기: ").append(android.os.Build.MANUFACTURER).append(' ').append(android.os.Build.MODEL)
                .append(", 온디바이스 ").append(deviceName.isEmpty() ? "-" : deviceName)
                .append(", 클라우드 ").append(AiManager.GEMINI_MODEL).append("\n");
        md.append("- JSON 방식 = 지금 게임(EventGenerator), 틀 방식 = TemplateEventGenerator. 보상 배수 ")
                .append(REWARD_MULTIPLIER).append("\n\n");

        md.append("## 요약\n\n| 대상/방식 | 성공 | 게임 문제 | 중앙값 | 느린 쪽 10% | 최대 |\n| --- | --- | --- | --- | --- | --- |\n");
        for (Map.Entry<String, List<Result>> e : results.entrySet()) {
            List<Result> list = e.getValue();
            List<Long> times = new ArrayList<>();
            int ok = 0, problems = 0;
            for (Result r : list) {
                times.add(r.ms);
                if (r.ok) ok++;
                if (!r.problems.isEmpty()) problems++;
            }
            Collections.sort(times);
            md.append("| ").append(e.getKey()).append(" | ").append(ok).append('/').append(list.size())
                    .append(" | ").append(problems)
                    .append(" | ").append(sec(times.get(times.size() / 2)))
                    .append(" | ").append(sec(times.get(Math.min(times.size() - 1, (int) Math.ceil(times.size() * 0.9) - 1))))
                    .append(" | ").append(sec(times.get(times.size() - 1))).append(" |\n");
        }

        md.append("\n## 경우별 결과\n");
        for (int i = 0; i < cases.size(); i++) {
            Case c = cases.get(i);
            md.append("\n### ").append(i + 1).append(". ").append(c.god.getGodName()).append(" ").append(c.floor).append("층\n");
            md.append("- 형상: ").append(c.plan.form.name).append(" (").append(c.plan.form.grade.label).append("), 장소: ")
                    .append(c.plan.place).append("\n");
            md.append("- 틀 방식 뼈대: ");
            for (int k = 0; k < c.plan.outcomes.size(); k++) {
                if (k > 0) md.append(" / ");
                md.append(k + 1).append(") ").append(TemplateEventGenerator.hint(c.plan.outcomes.get(k), c.plan.enemy));
            }
            md.append("\n");
            for (Map.Entry<String, List<Result>> e : results.entrySet()) {
                Result r = e.getValue().get(i);
                md.append("\n**").append(e.getKey()).append("** (").append(sec(r.ms)).append(")");
                if (!r.ok) {
                    md.append(" 실패: ").append(r.error).append("\n");
                    continue;
                }
                if (!r.problems.isEmpty()) md.append(" 문제: ").append(r.problems);
                md.append("\n");
                GameEvent ev = r.event;
                md.append("- 이름: ").append(ev.getName()).append("\n");
                md.append("- 상황: ").append(ev.getDescription()).append("\n");
                if (ev instanceof BattleEvent) md.append("- 적: ").append(((BattleEvent) ev).getEnemyId()).append("\n");
                for (int k = 0; k < ev.getChoices().size(); k++) {
                    Reward rw = k < ev.getRewards().size() ? ev.getRewards().get(k) : null;
                    md.append("- 선택지").append(k + 1).append(": ").append(ev.getChoices().get(k));
                    if (rw != null) {
                        md.append(" → ").append(rw.getDescription());
                        md.append(" `").append(rewardCode(rw)).append("`");
                    }
                    md.append("\n");
                }
            }
        }

        File dir = new File(getExternalFilesDir(null), "spike");
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("폴더를 못 만듦: " + dir);
        File file = new File(dir, "event_" + stamp + ".md");
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(md.toString().getBytes(StandardCharsets.UTF_8));
        }
        return file;
    }

    private static String rewardCode(Reward r) {
        List<String> parts = new ArrayList<>();
        if (r.getAction() != null) parts.add(r.getAction());
        if (r.getItemId() != null) parts.add(r.getItemId());
        if (r.getStatRewards() != null) {
            for (RewardStat s : r.getStatRewards()) parts.add(s.getStatType() + " " + s.getValue());
        }
        return parts.isEmpty() ? "없음" : String.join(", ", parts);
    }

    private static String sec(long ms) {
        return String.format(Locale.US, "%.1f초", ms / 1000.0);
    }
}
