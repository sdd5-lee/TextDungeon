package com.textdungeon.ai;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.textdungeon.ai.backend.LlmBackend;
import com.textdungeon.ai.backend.LlmCallback;
import com.textdungeon.ai.backend.ResponseFormat;
import com.textdungeon.data.EventGson;
import com.textdungeon.event.GameEvent;
import com.textdungeon.model.Item;
import com.textdungeon.model.Monster;
import com.textdungeon.model.Reward;
import com.textdungeon.model.RewardStat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 틀 방식 이벤트 생성.
 * 이벤트의 뼈대(종류, 상대 몬스터, 선택지별 보상 수치와 아이템)는 코드가 신의 성격에 맞춰 먼저 정하고,
 * AI는 그 결과에 맞는 이름·상황·선택지·결과 묘사 여섯 줄만 쓴다.
 * JSON 방식(EventGenerator)은 AI가 JSON 전체를 쓰고 코드가 고치는데, 출력이 길어 느리고 작은 모델은 형식을 자주 틀린다.
 * 보상 수치는 주사위 결과표처럼 층별 상한(RewardLimits)의 %로 정하고, 실제 적용량은 결과 묘사 끝에 붙인다.
 */
public class TemplateEventGenerator {
    private static final String TAG = "TemplateEvent";
    /** 여섯 줄이면 한국어 300자 안팎이라 이 정도면 충분하다 */
    static final int MAX_OUTPUT_TOKENS = 400;
    /**
     * 기본 온도에서는 한국어 사이에 다른 나라 글자가 섞여 20개 중 7개를 버렸다. 0.6에서는 4개, 0.4로 더 낮춰도 나아지지 않았다
     * (S24, docs/ondevice-event). 주사위는 기본값 그대로 쓴다
     */
    static final double TEMPERATURE = 0.6;

    static final int MAX_NAME_LENGTH = 24;
    static final int MAX_CHOICE_LENGTH = 30;
    static final int MAX_TEXT_LENGTH = 160;
    /** 묘사가 문장인지 보는 최소 한글 글자 수 */
    private static final int MIN_TEXT_HANGUL = 8;

    private static final String[] TRAINING_STATS = {"힘", "민첩", "체력", "지혜"};
    private static final String[] GAINS = {"힘", "민첩", "체력", "지혜", "경험치", "골드", "회복"};
    private static final String[] COSTS = {"데미지", "힘", "민첩", "체력", "지혜", "골드"};

    // 형식 예시. 그대로 베끼면 버린다. 빛보다 장소와 물건을 그리는 문장으로 보여 준다
    static final String EXAMPLE_NAME = "녹슨 저울의 심판";
    static final String EXAMPLE_SITUATION = "안개 낀 돌다리 끝에 녹슨 저울 하나가 삐걱거리고, 낮은 목소리가 무엇을 올려놓겠느냐고 묻는다.";
    static final String EXAMPLE_CHOICE1 = "저울에 피 한 방울을 떨어뜨린다";
    static final String EXAMPLE_RESULT1 = "저울이 기울며 손끝이 차갑게 저려 오지만, 핏줄을 따라 낯선 힘이 스며든다.";
    static final String EXAMPLE_CHOICE2 = "저울을 내버려 두고 돌아선다";
    static final String EXAMPLE_RESULT2 = "저울이 멈추고, 다리 위로 은화 몇 닢이 굴러떨어진다.";

    private static final String[] KEYS = {"이름", "상황", "선택지1", "결과1", "선택지2", "결과2"};
    // "**선택지 1**: ...", "- 결과1 : ..." 같은 변형도 받는다
    private static final Pattern LINE = Pattern.compile("^[\\s*#>-]*(이름|상황|선택지\\s*[12]|결과\\s*[12])\\s*\\**\\s*[:：]\\s*(.+)$");
    // 작은 모델이 한국어 사이에 다른 글자를 섞는다 ("hardening되지만", "미 微動하며", "위험Ни를", "두려움वश", "진_한").
    // 섞이는 글자가 여러 나라 것이라 한글·숫자·문장부호 말고는 모두 거부한다. 게임 이름에는 영문이 없고 HP만 쓴다
    private static final Pattern FOREIGN = Pattern.compile("[^가-힣ㄱ-ㅎㅏ-ㅣ0-9\\s.,!?'\"()\\[\\]~·…‘’“”:;%/—–-]");
    private static final Pattern PLACEHOLDER =Pattern.compile("^[(\\[<（【].*|.*\\d+\\s*자\\s*(내외|이내|안팎).*");

    /** 선택지 하나의 정해진 결과 */
    static class Outcome {
        String action;        // battle, escape, null
        String itemId;
        String itemName;
        /** 시련의 신 결투에서 이기면 오르는 스탯. 양은 싸울 때의 스탯으로 정하므로 stats에는 넣지 않는다 (GodTrial) */
        String trialStat;
        final List<RewardStat> stats = new ArrayList<>();
        /** 종류별로 층 상한의 몇 %로 정했는지. 프롬프트의 "크게/꽤/조금"을 고르는 데 쓴다 */
        final Map<String, Integer> percents = new java.util.HashMap<>();
    }

    /** 코드가 먼저 정한 이벤트 뼈대 */
    static class Plan {
        AiType god;
        int floor;
        GodForms.Form form;   // 신이 내미는 형상. 등급이 보상 세기를 정한다
        String place;         // 꿈속 천상에서 신이 머무는 곳
        String type;          // battle, normal
        Monster enemy;
        final List<Outcome> outcomes = new ArrayList<>();
    }

    static class ParseException extends Exception {
        ParseException(String message) { super(message); }
    }

    private final LlmBackend backend;
    private final Gson eventGson = EventGson.create();
    private final Random random;

    public TemplateEventGenerator(LlmBackend backend) {
        this(backend, new Random());
    }

    TemplateEventGenerator(LlmBackend backend, Random random) {
        this.backend = backend;
        this.random = random;
    }

    /**
     * 아이템 후보는 형상 등급마다 다르다: 하급은 이 층대 아이템, 중급은 6~15층 깊은 장비, 상급은 심층 장비.
     * @param nearItems        이 층대 아이템 (하급)
     * @param upperItems       지금보다 깊은 층의 장비 (중급)
     * @param deepItems        심층 장비 (상급)
     * @param monsters         이 층대 몬스터 (투쟁의 신, 시련의 신 상대 후보)
     * @param rewardMultiplier 던전 보상 배수, expMultiplier 특성 경험치 배수. 결과 묘사 끝의 실제 적용량 요약에 쓴다
     */
    public void generate(int floor, AiType god, List<Item> nearItems, List<Item> upperItems, List<Item> deepItems,
                         List<Monster> monsters, int rewardMultiplier, int expMultiplier, AiCallback callback) {
        GodForms.Form form = GodForms.pick(god, random);
        Plan plan = plan(god, floor, form, itemsFor(form.grade, nearItems, upperItems, deepItems), monsters, random);
        backend.generate(prompt(plan), ResponseFormat.TEXT, MAX_OUTPUT_TOKENS, TEMPERATURE, new LlmCallback() {
            @Override
            public void onResult(String text) {
                finish(text, plan, rewardMultiplier, expMultiplier, MAX_REPAIR_ROUNDS, callback);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    /**
     * 실패의 대부분은 여섯 줄 중 한 줄에 외국 단어가 섞인 것이다. 같은 뼈대로 다시 만들면 같은 자리에서 또 깨지지만
     * (S24, 10개 중 1개 성공, 6.3초), 그 줄만 고쳐 쓰게 하면 10개 중 9개가 2.2초에 고쳐졌다 (docs/ondevice-event)
     */
    static final int MAX_REPAIR_ROUNDS = 2;

    /** 응답을 이벤트로 바꾼다. 한 줄만 고치면 되는 문제면 그 줄들을 고쳐 쓰게 하고 다시 본다 */
    private void finish(String text, Plan plan, int rewardMultiplier, int expMultiplier, int repairsLeft,
                        AiCallback callback) {
        try {
            callback.onSuccess(toEvent(text, plan, rewardMultiplier, expMultiplier));
            return;
        } catch (ParseException e) {
            Map<String, String> lines = extractLines(text);
            List<Broken> broken = repairsLeft > 0 ? brokenLines(lines) : null;
            if (broken != null && !broken.isEmpty()) {
                Log.i(TAG, "깨진 줄 " + broken.size() + "개 고쳐 쓰기: " + e.getMessage());
                repair(lines, broken, 0, () -> finish(joinLines(lines), plan, rewardMultiplier, expMultiplier,
                        repairsLeft - 1, callback), callback);
                return;
            }
            Log.w(TAG, "틀 방식 응답을 쓸 수 없음: " + e.getMessage() + "\n" + text);
            callback.onError("이벤트 창조 실패: " + e.getMessage());
        } catch (Exception e) {
            Log.w(TAG, "틀 방식 응답을 쓸 수 없음: " + e.getMessage() + "\n" + text);
            callback.onError("이벤트 창조 실패: " + e.getMessage());
        }
    }

    /** 깨진 줄을 하나씩 차례로 고쳐 쓴다 (모델은 요청을 한 번에 하나만 받는다) */
    private void repair(Map<String, String> lines, List<Broken> broken, int index, Runnable done, AiCallback callback) {
        if (index >= broken.size()) {
            done.run();
            return;
        }
        Broken b = broken.get(index);
        backend.generate(repairPrompt(lines, b), ResponseFormat.TEXT, REPAIR_MAX_OUTPUT_TOKENS, TEMPERATURE,
                new LlmCallback() {
                    @Override
                    public void onResult(String text) {
                        lines.put(b.key, repairedValue(text, b.key));
                        repair(lines, broken, index + 1, done, callback);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        callback.onError(errorMessage);
                    }
                });
    }

    // ── 뼈대 ─────────────────────────────────────

    /** 등급에 맞는 아이템 후보. 비어 있으면 가까운 등급 것을 쓴다 */
    static List<Item> itemsFor(GodForms.Grade grade, List<Item> nearItems, List<Item> upperItems, List<Item> deepItems) {
        List<List<Item>> order;
        if (grade == GodForms.Grade.LOW) order = Arrays.asList(nearItems, upperItems, deepItems);
        else if (grade == GodForms.Grade.MID) order = Arrays.asList(upperItems, deepItems, nearItems);
        else order = Arrays.asList(deepItems, upperItems, nearItems);
        for (List<Item> items : order) {
            if (items != null && !items.isEmpty()) return items;
        }
        return Collections.emptyList();
    }

    /**
     * 신의 성격이 보상의 모양(무엇을 얻고 무엇을 바치는지)을, 형상 등급이 얻는 쪽의 세기와 아이템 등급을 정한다.
     * 대가(데미지, 스탯 감소)는 등급과 상관없이 신의 성격대로다.
     * @param items 형상 등급에 맞는 아이템 후보 (itemsFor)
     */
    static Plan plan(AiType god, int floor, GodForms.Form form, List<Item> items, List<Monster> monsters, Random rnd) {
        Plan p = new Plan();
        p.god = god;
        p.floor = Math.max(1, floor);
        p.form = form;
        p.type = "normal";
        Outcome first = new Outcome();
        Outcome second = new Outcome();
        p.outcomes.add(first);
        p.outcomes.add(second);
        int lo = form.grade.minPercent;
        int hi = form.grade.maxPercent;

        switch (god) {
            case STRUGGLE:
                if (monsters != null && !monsters.isEmpty()) {
                    p.type = "battle";
                    p.enemy = monsters.get(rnd.nextInt(monsters.size()));
                    first.action = "battle";
                    second.action = "escape";
                    // 이기면 스탯 두 개와 전리품. 도망치면 아무것도 없다
                    for (String s : pickDistinct(TRAINING_STATS, 2, rnd)) add(first, s, between(lo, hi, rnd), p.floor);
                    setItem(first, pickItem(items, rnd));
                } else {
                    // 이 층대에 몬스터가 없으면 결투 대신 수련 (나올 일은 거의 없다)
                    add(first, pick(TRAINING_STATS, rnd), between(lo, hi, rnd), p.floor);
                    add(first, "데미지", between(40, 70, rnd), p.floor);
                    setItem(first, pickItem(items, rnd));
                    add(second, "경험치", between(lo / 2, hi / 2, rnd), p.floor);
                }
                break;
            case MAGIC:
            case THIEF:
            case STRENGTH:
            case TRAINING:
                if (monsters != null && !monsters.isEmpty()) {
                    // 신이 그 스탯을 시험하는 결투. 이벤트는 던전에 들어갈 때 만들어 두므로
                    // 적의 세기와 오르는 양은 여기서 정하지 않고 싸울 때의 스탯으로 정한다 (GodTrial)
                    p.type = "battle";
                    p.enemy = monsters.get(rnd.nextInt(monsters.size()));
                    first.action = "battle";
                    first.trialStat = god.getTrialStat();
                    second.action = "escape";
                } else {
                    // 몬스터가 없으면 결투 대신 수련 (나올 일은 거의 없다)
                    add(first, god.getTrialStat(), between(lo, hi, rnd), p.floor);
                    add(first, "데미지", between(40, 70, rnd), p.floor);
                }
                break;
            case TREASURE:
                List<Item> pool = new ArrayList<>(items == null ? Collections.emptyList() : items);
                pool.removeIf(i -> i == null || i.getId() == null);
                Collections.shuffle(pool, rnd);
                if (pool.size() >= 2) {
                    setItem(first, pool.get(0));
                    setItem(second, pool.get(1));
                } else {
                    if (!pool.isEmpty()) setItem(first, pool.get(0));
                    add(second, "골드", between(lo, hi, rnd), p.floor);
                }
                break;
            case ADVENTURE:
                // 큰 위험 + 큰 보상 / 작은 위험 + 작은 보상
                add(first, "데미지", between(70, 100, rnd), p.floor);
                add(first, pick(TRAINING_STATS, rnd), between(lo, hi, rnd), p.floor);
                Item found = pickItem(items, rnd);
                if (found != null) setItem(first, found);
                else add(first, "골드", between(lo, hi, rnd), p.floor);
                add(second, "데미지", between(15, 35, rnd), p.floor);
                add(second, rnd.nextBoolean() ? "경험치" : "골드", between(lo / 2, hi / 2, rnd), p.floor);
                break;
            case DEATH:
                // 생명을 크게 바치고 금기된 힘과 유물 / 조금 바치고 작은 힘
                add(first, "데미지", between(85, 100, rnd), p.floor);
                for (String s : pickDistinct(TRAINING_STATS, 2, rnd)) add(first, s, between(lo, hi, rnd), p.floor);
                setItem(first, pickItem(items, rnd));
                add(second, "데미지", between(35, 55, rnd), p.floor);
                add(second, pick(TRAINING_STATS, rnd), between(lo, hi, rnd), p.floor);
                break;
            case CHAOS:
            default:
                // 두 선택지 모두 아주 작거나 아주 큰 보상, 대가는 있을 수도 없을 수도.
                // 등급이 높을수록 아주 큰 쪽이 자주 나온다 (하급 1/4, 중급 1/2, 상급 3/4)
                int hugeChance = form.grade.ordinal() + 1;
                for (Outcome o : p.outcomes) {
                    if (rnd.nextInt(4) == 0 && items != null && !items.isEmpty()) {
                        setItem(o, pickItem(items, rnd));
                    } else {
                        boolean huge = rnd.nextInt(4) < hugeChance;
                        add(o, pick(GAINS, rnd), huge ? between(80, 100, rnd) : between(5, 20, rnd), p.floor);
                    }
                    if (rnd.nextBoolean()) {
                        String cost = pick(COSTS, rnd);
                        int percent = between(20, 100, rnd);
                        add(o, cost, "데미지".equals(cost) ? percent : -percent, p.floor);
                    }
                }
                break;
        }
        // 보상을 다 정한 뒤에 고른다 (장소를 넣기 전 측정과 같은 시드면 보상이 같다)
        p.place = GodForms.place(god, rnd);
        return p;
    }

    private static void add(Outcome o, String type, int percent, int floor) {
        int value = RewardLimits.scaled(type, percent, floor, ChaosTable.NORMAL_LIMIT_PERCENT);
        if (value == 0) return;
        if (o.percents.containsKey(type)) return; // 같은 종류는 한 번만
        o.stats.add(new RewardStat(type, value));
        o.percents.put(type, Math.abs(percent));
    }

    private static void setItem(Outcome o, Item item) {
        if (item == null) return;
        o.itemId = item.getId();
        o.itemName = item.getName();
    }

    private static Item pickItem(List<Item> items, Random rnd) {
        if (items == null || items.isEmpty()) return null;
        return items.get(rnd.nextInt(items.size()));
    }

    private static String pick(String[] values, Random rnd) {
        return values[rnd.nextInt(values.length)];
    }

    private static List<String> pickDistinct(String[] values, int count, Random rnd) {
        List<String> list = new ArrayList<>(Arrays.asList(values));
        Collections.shuffle(list, rnd);
        return list.subList(0, Math.min(count, list.size()));
    }

    private static int between(int from, int to, Random rnd) {
        return from + rnd.nextInt(to - from + 1);
    }

    // ── 프롬프트 ─────────────────────────────────

    static String prompt(Plan p) {
        StringBuilder sb = new StringBuilder();
        // 신들의 이벤트는 꿈속 천상에서 신이 내리는 축복이다. 층 숫자를 주면 상황에 "21층"을 그대로 써서 주지 않는다
        sb.append("[꿈속 천상에서 ").append(p.god.getGodName()).append("이 내리는 ")
                .append(p.god.isTrial() ? "시련" : "축복").append("]\n");
        sb.append("장소: 천상의 ").append(p.place).append("\n");
        sb.append("신의 성격: ").append(p.god.getTone()).append("\n");
        sb.append("신이 내미는 형상: ").append(p.form.name).append(" (").append(p.form.grade.label).append(")\n");
        if (p.enemy != null) {
            sb.append("등장하는 적: ").append(p.enemy.getName())
                    .append(". 선택지1은 이 적과 싸우는 행동, 선택지2는 싸움을 피하는 행동.\n");
        }
        if (p.god == AiType.TREASURE && p.outcomes.get(0).itemName != null) {
            sb.append("보물 둘 중 하나만 가질 수 있다. 각 선택지는 그 보물을 고르는 행동.\n");
        }
        sb.append("선택지별로 이미 정해진 결과:\n");
        for (int i = 0; i < p.outcomes.size(); i++) {
            sb.append(i + 1).append(". ").append(hint(p.outcomes.get(i), p.enemy)).append("\n");
        }
        sb.append("\n이 결과에 맞는 이벤트를 지어라. 아래 예시와 같은 형식으로 여섯 줄만 답하라.\n\n")
                .append("이름: ").append(EXAMPLE_NAME).append("\n")
                .append("상황: ").append(EXAMPLE_SITUATION).append("\n")
                .append("선택지1: ").append(EXAMPLE_CHOICE1).append("\n")
                .append("결과1: ").append(EXAMPLE_RESULT1).append("\n")
                .append("선택지2: ").append(EXAMPLE_CHOICE2).append("\n")
                .append("결과2: ").append(EXAMPLE_RESULT2).append("\n\n")
                .append("규칙: 예시의 문장은 쓰지 말고 새로 지어라. 이름은 형상이 드러나게 15자 안팎, ")
                .append("상황은 그 장소에서 신이 형상을 내미는 한두 문장이고 형상 이름을 그대로 쓴다. ")
                .append("빛보다 장소와 물건을 그린다. 선택지는 20자 안팎의 행동. ")
                .append("결과는 위에 정해진 변화를 이야기로 묘사하고 숫자는 쓰지 않는다. 두 선택지는 서로 달라야 한다. ")
                .append("한국어로만 쓰고 영어·한자는 쓰지 않는다.");
        return sb.toString();
    }

    /** 정해진 결과를 숫자 없는 말로. 예: "힘이 크게 오르지만 HP를 많이 잃는다" */
    static String hint(Outcome o, Monster enemy) {
        List<String> gains = new ArrayList<>();
        List<String> losses = new ArrayList<>();
        if (o.itemName != null) gains.add("'" + o.itemName + "'을(를) 얻는다");
        if (o.trialStat != null) gains.add(o.trialStat + "이(가) 오른다");
        for (RewardStat s : o.stats) {
            String type = s.getStatType();
            int v = s.getValue();
            int percent = o.percents.getOrDefault(type, 50);
            if ("데미지".equals(type)) losses.add("HP를 " + amount(percent, true) + " 잃는다");
            else if ("회복".equals(type)) gains.add("HP를 " + amount(percent, false) + " 회복한다");
            else if ("경험치".equals(type)) gains.add("경험치를 " + amount(percent, false) + " 얻는다");
            else if ("골드".equals(type)) {
                if (v > 0) gains.add("골드를 " + amount(percent, false) + " 얻는다");
                else losses.add("골드를 잃는다");
            } else if (v > 0) gains.add(type + "이(가) " + amount(percent, false) + " 오른다");
            else losses.add(type + "이(가) 떨어진다");
        }
        String result;
        if (gains.isEmpty() && losses.isEmpty()) result = "아무것도 얻지 못한다";
        else if (losses.isEmpty()) result = String.join(", ", gains);
        else if (gains.isEmpty()) result = String.join(", ", losses);
        else result = String.join(", ", gains) + ". 대신 " + String.join(", ", losses);

        if ("battle".equals(o.action) && enemy != null) return "(전투) " + enemy.getName() + "과(와) 싸운다. 이기면 " + result;
        if ("escape".equals(o.action)) return "(도망) 싸우지 않고 물러난다. " + result;
        return result;
    }

    /** 층 상한의 몇 %인지로 크기 말을 고른다 */
    private static String amount(int percent, boolean loss) {
        if (percent >= 80) return loss ? "많이" : "크게";
        if (percent >= 40) return "꽤";
        return "조금";
    }

    // ── 응답 → 이벤트 ────────────────────────────

    /** AI 응답 여섯 줄을 꺼낸다. 하나라도 없거나 규칙을 어기면 ParseException */
    static Map<String, String> parseLines(String raw) throws ParseException {
        Map<String, String> found = extractLines(raw);
        for (String key : KEYS) {
            if (!found.containsKey(key)) throw new ParseException(key + " 줄 없음");
        }
        // 이름, 선택지를 먼저 본다
        for (String key : new String[]{"이름", "선택지1", "선택지2", "상황", "결과1", "결과2"}) {
            checkLine(key, found.get(key));
        }
        if (normalize(found.get("선택지1")).equals(normalize(found.get("선택지2")))) {
            throw new ParseException("두 선택지가 같음");
        }
        return found;
    }

    /** 검사 없이 "키: 값" 줄을 꺼낸다 */
    static Map<String, String> extractLines(String raw) {
        Map<String, String> found = new java.util.LinkedHashMap<>();
        if (raw == null) return found;
        for (String line : raw.split("\\r?\\n")) {
            Matcher m = LINE.matcher(line.trim());
            if (!m.matches()) continue;
            String key = m.group(1).replaceAll("\\s", "");
            // 굵은 글씨 표시를 지우고, 단어 사이에 생긴 긴 공백("진        한")은 한 칸으로 줄인다
            String value = m.group(2).replaceAll("\\*+", "").replaceAll("\\s+", " ").trim();
            if (!found.containsKey(key) && !value.isEmpty()) found.put(key, value);
        }
        return found;
    }

    private static void checkLine(String key, String value) throws ParseException {
        switch (key) {
            case "이름": check(value, key, MAX_NAME_LENGTH, EXAMPLE_NAME); break;
            case "선택지1": check(value, key, MAX_CHOICE_LENGTH, EXAMPLE_CHOICE1); break;
            case "선택지2": check(value, key, MAX_CHOICE_LENGTH, EXAMPLE_CHOICE2); break;
            case "상황": checkText(value, key, EXAMPLE_SITUATION); break;
            case "결과1": checkText(value, key, EXAMPLE_RESULT1); break;
            default: checkText(value, key, EXAMPLE_RESULT2); break;
        }
    }

    // ── 깨진 줄 고쳐 쓰기 ─────────────────────────
    // 실패는 대개 여섯 줄 중 한 줄에 외국 단어 하나가 섞인 것이라, 전체를 다시 만들지 않고 그 줄만 고쳐 쓰게 한다

    /** 고쳐 쓸 한 줄. 고친 줄은 parseLines로 다시 검사한다 */
    static final class Broken {
        final String key;
        final String value;
        /** 모델에게 줄 고칠 내용 */
        final String instruction;

        Broken(String key, String value, String instruction) {
            this.key = key;
            this.value = value;
            this.instruction = instruction;
        }
    }

    static final int REPAIR_MAX_OUTPUT_TOKENS = 160;
    private static final Pattern FOREIGN_RUN = Pattern.compile(FOREIGN.pattern() + "+");

    /**
     * 규칙을 어긴 줄들. 외국 글자, 길이, 숫자처럼 그 줄만 고치면 되는 문제만 고른다.
     * 줄이 없거나 예시를 베꼈거나 두 선택지가 같으면 null (다시 만든다). 어긴 줄이 없으면 빈 목록
     */
    static List<Broken> brokenLines(Map<String, String> lines) {
        List<Broken> broken = new ArrayList<>();
        for (String key : KEYS) {
            String value = lines.get(key);
            if (value == null) return null;
            try {
                checkLine(key, value);
            } catch (ParseException e) {
                String instruction = instruction(key, value, e.getMessage());
                if (instruction == null) return null;
                broken.add(new Broken(key, value, instruction));
            }
        }
        if (broken.isEmpty() && normalize(lines.get("선택지1")).equals(normalize(lines.get("선택지2")))) return null;
        return broken;
    }

    private static String instruction(String key, String value, String problem) {
        if (problem.contains("외국 글자")) {
            List<String> parts = new ArrayList<>();
            Matcher m = FOREIGN_RUN.matcher(value.replace("HP", ""));
            while (m.find()) {
                if (!parts.contains("'" + m.group() + "'")) parts.add("'" + m.group() + "'");
            }
            return String.join(", ", parts) + "은(는) 한국어가 아니다. 그 부분만 문맥에 맞는 한국어로 바꾸고 나머지 글자는 그대로 둔다.";
        }
        if (problem.contains("자를 넘음")) {
            int limit = key.startsWith("선택지") ? 20 : key.equals("이름") ? 15 : 100;
            return "너무 길다. 뜻은 그대로 두고 " + limit + "자 안팎으로 줄인다.";
        }
        if (problem.contains("숫자가 있음")) return "숫자를 빼고 말로만 쓴다.";
        return null;
    }

    /** 이벤트 글 전체를 맥락으로 주고, 깨진 한 줄만 고쳐 쓰게 한다 */
    static String repairPrompt(Map<String, String> lines, Broken b) {
        StringBuilder sb = new StringBuilder("아래는 던전 게임 이벤트 글이다.\n");
        for (String key : KEYS) sb.append(key).append(": ").append(lines.get(key)).append("\n");
        sb.append("\n'").append(b.key).append("' 줄에서 ").append(b.instruction).append("\n")
                .append("고친 ").append(b.key).append(" 한 줄만 '").append(b.key).append(": ' 뒤에 써서 답하라. 한국어로만 쓴다.");
        return sb.toString();
    }

    /** 고쳐 쓴 답에서 그 줄을 꺼낸다. "키:"를 빼먹었으면 첫 줄을 쓴다 */
    static String repairedValue(String raw, String key) {
        String value = extractLines(raw).get(key);
        if (value != null) return value;
        for (String line : raw.split("\\r?\\n")) {
            String v = line.replaceAll("^[\\s*#>\"'-]+|[\\s*\"']+$", "").replaceAll("\\s+", " ");
            if (!v.isEmpty()) return v;
        }
        return "";
    }

    /** 꺼낸 줄을 다시 응답 모양으로 (parseLines, toEvent에 넣는다) */
    static String joinLines(Map<String, String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String key : KEYS) sb.append(key).append(": ").append(lines.get(key)).append("\n");
        return sb.toString();
    }

    private static void check(String value, String key, int maxLength, String example) throws ParseException {
        if (value.length() > maxLength) throw new ParseException(key + "이 " + maxLength + "자를 넘음: " + value);
        if (PLACEHOLDER.matcher(value).matches()) throw new ParseException(key + "이 안내문 같음: " + value);
        if (normalize(value).equals(normalize(example))) throw new ParseException(key + "이 예시를 베낌");
        if (FOREIGN.matcher(value.replace("HP", "")).find()) throw new ParseException(key + "에 외국 글자가 섞임: " + value);
    }

    private static void checkText(String value, String key, String example) throws ParseException {
        check(value, key, MAX_TEXT_LENGTH, example);
        // 수치는 코드가 붙이므로, 묘사에 숫자가 있으면 실제 적용량과 어긋날 수 있다. "21층" 같은 층수는 괜찮다
        if (value.replaceAll("\\d+\\s*층", "").matches(".*\\d.*")) throw new ParseException(key + "에 숫자가 있음: " + value);
        if (value.replaceAll("[^가-힣]", "").length() < MIN_TEXT_HANGUL) {
            throw new ParseException(key + "이 문장이 아님: " + value);
        }
    }

    private static String normalize(String s) {
        return s.replaceAll("[\\s.,!?'\"]", "");
    }

    GameEvent toEvent(String raw, Plan p, int rewardMultiplier, int expMultiplier) throws ParseException {
        Map<String, String> lines = parseLines(raw);
        JsonObject event = new JsonObject();
        event.addProperty("id", p.god.getIdPrefix() + p.floor);
        event.addProperty("name", lines.get("이름"));
        event.addProperty("description", lines.get("상황"));
        event.addProperty("imgId", "");
        event.addProperty("minFloor", p.floor);
        event.addProperty("maxFloor", p.floor);
        event.addProperty("type", p.type);
        if (p.enemy != null) event.addProperty("enemyId", p.enemy.getId());
        else event.add("enemyId", JsonNull.INSTANCE);
        event.add("shopItems", JsonNull.INSTANCE);

        JsonArray choices = new JsonArray();
        JsonArray rewards = new JsonArray();
        for (int i = 0; i < p.outcomes.size(); i++) {
            Outcome o = p.outcomes.get(i);
            if (o.trialStat != null) event.addProperty("trialStat", o.trialStat);
            choices.add(lines.get("선택지" + (i + 1)));
            String summary = new Reward(null, o.itemId, o.stats).summary(rewardMultiplier, expMultiplier, o.itemName);
            String description = lines.get("결과" + (i + 1)) + (summary.isEmpty() ? "" : " " + summary);

            JsonObject reward = new JsonObject();
            if (o.action != null) reward.addProperty("action", o.action);
            else reward.add("action", JsonNull.INSTANCE);
            if (o.itemId != null) reward.addProperty("itemId", o.itemId);
            else reward.add("itemId", JsonNull.INSTANCE);
            reward.addProperty("description", description);
            JsonArray stats = new JsonArray();
            for (RewardStat s : o.stats) {
                JsonObject so = new JsonObject();
                so.addProperty("type", s.getStatType());
                so.addProperty("value", s.getValue());
                stats.add(so);
            }
            reward.add("statRewards", stats);
            rewards.add(reward);
        }
        event.add("choices", choices);
        event.add("rewards", rewards);
        com.textdungeon.ai.backend.GeminiBackend.logRaw("틀 방식 이벤트", event.toString());
        return eventGson.fromJson(event, GameEvent.class);
    }
}
