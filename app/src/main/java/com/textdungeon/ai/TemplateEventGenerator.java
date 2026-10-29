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

    // 형식 예시. 그대로 베끼면 버린다
    static final String EXAMPLE_NAME = "잿빛 깃털의 약속";
    static final String EXAMPLE_SITUATION = "허공에서 잿빛 깃털 하나가 내려앉고, 낮은 목소리가 대가를 치를 각오가 있느냐고 묻는다.";
    static final String EXAMPLE_CHOICE1 = "깃털을 힘껏 움켜쥔다";
    static final String EXAMPLE_RESULT1 = "깃털이 손바닥을 파고들어 피가 흐르지만, 상처 사이로 낯선 힘이 스며든다.";
    static final String EXAMPLE_CHOICE2 = "고개를 숙여 사양한다";
    static final String EXAMPLE_RESULT2 = "목소리가 웃음을 흘리며 사라지고, 발치에 은화 몇 닢이 남는다.";

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
        final List<RewardStat> stats = new ArrayList<>();
        /** 종류별로 층 상한의 몇 %로 정했는지. 프롬프트의 "크게/꽤/조금"을 고르는 데 쓴다 */
        final Map<String, Integer> percents = new java.util.HashMap<>();
    }

    /** 코드가 먼저 정한 이벤트 뼈대 */
    static class Plan {
        AiType god;
        int floor;
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
     * @param nearItems        이 층대 아이템 (모험·혼돈의 신 보상 후보)
     * @param treasureItems    심층 장비 (보물의 신 보상 후보)
     * @param monsters         이 층대 몬스터 (투쟁의 신 상대 후보)
     * @param rewardMultiplier 난이도 보상 배수, expMultiplier 특성 경험치 배수. 결과 묘사 끝의 실제 적용량 요약에 쓴다
     */
    public void generate(int floor, AiType god, List<Item> nearItems, List<Item> treasureItems, List<Monster> monsters,
                         int rewardMultiplier, int expMultiplier, AiCallback callback) {
        Plan plan = plan(god, floor, nearItems, treasureItems, monsters, random);
        backend.generate(prompt(plan), ResponseFormat.TEXT, MAX_OUTPUT_TOKENS, TEMPERATURE, new LlmCallback() {
            @Override
            public void onResult(String text) {
                try {
                    callback.onSuccess(toEvent(text, plan, rewardMultiplier, expMultiplier));
                } catch (Exception e) {
                    Log.w(TAG, "틀 방식 응답을 쓸 수 없음: " + e.getMessage() + "\n" + text);
                    callback.onError("이벤트 창조 실패: " + e.getMessage());
                }
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    // ── 뼈대 ─────────────────────────────────────

    static Plan plan(AiType god, int floor, List<Item> nearItems, List<Item> treasureItems, List<Monster> monsters,
                     Random rnd) {
        Plan p = new Plan();
        p.god = god;
        p.floor = Math.max(1, floor);
        p.type = "normal";
        Outcome first = new Outcome();
        Outcome second = new Outcome();
        p.outcomes.add(first);
        p.outcomes.add(second);

        switch (god) {
            case STRUGGLE:
                if (monsters != null && !monsters.isEmpty()) {
                    p.type = "battle";
                    p.enemy = monsters.get(rnd.nextInt(monsters.size()));
                    first.action = "battle";
                    second.action = "escape";
                    // 이기면 스탯 두 개가 크게 오른다. 도망치면 아무것도 없다
                    List<String> stats = pickDistinct(TRAINING_STATS, 2, rnd);
                    for (String s : stats) add(first, s, between(70, 100, rnd), p.floor);
                } else {
                    // 이 층대에 몬스터가 없으면 결투 대신 수련 (나올 일은 거의 없다)
                    add(first, pick(TRAINING_STATS, rnd), between(60, 90, rnd), p.floor);
                    add(first, "데미지", between(40, 70, rnd), p.floor);
                    add(second, "경험치", between(20, 40, rnd), p.floor);
                }
                break;
            case TREASURE:
                List<Item> pool = new ArrayList<>(treasureItems == null ? Collections.emptyList() : treasureItems);
                pool.removeIf(i -> i == null || i.getId() == null);
                Collections.shuffle(pool, rnd);
                if (pool.size() >= 2) {
                    setItem(first, pool.get(0));
                    setItem(second, pool.get(1));
                } else {
                    if (!pool.isEmpty()) setItem(first, pool.get(0));
                    add(second, "골드", between(60, 100, rnd), p.floor);
                }
                break;
            case ADVENTURE:
                // 큰 위험 + 큰 보상 / 작은 위험 + 작은 보상
                add(first, "데미지", between(70, 100, rnd), p.floor);
                add(first, pick(TRAINING_STATS, rnd), between(80, 100, rnd), p.floor);
                if (rnd.nextBoolean()) setItem(first, pickItem(nearItems, rnd));
                else add(first, "골드", between(60, 100, rnd), p.floor);
                add(second, "데미지", between(15, 35, rnd), p.floor);
                add(second, rnd.nextBoolean() ? "경험치" : "골드", between(30, 50, rnd), p.floor);
                break;
            case DEATH:
                // 생명을 크게 바치고 금기된 힘 / 조금 바치고 작은 힘
                add(first, "데미지", between(85, 100, rnd), p.floor);
                for (String s : pickDistinct(TRAINING_STATS, 2, rnd)) add(first, s, between(80, 100, rnd), p.floor);
                add(second, "데미지", between(35, 55, rnd), p.floor);
                add(second, pick(TRAINING_STATS, rnd), between(40, 60, rnd), p.floor);
                break;
            case CHAOS:
            default:
                // 두 선택지 모두 아주 작거나 아주 큰 보상, 대가는 있을 수도 없을 수도
                for (Outcome o : p.outcomes) {
                    if (rnd.nextInt(4) == 0 && nearItems != null && !nearItems.isEmpty()) {
                        setItem(o, pickItem(nearItems, rnd));
                    } else {
                        add(o, pick(GAINS, rnd), rnd.nextBoolean() ? between(5, 20, rnd) : between(80, 100, rnd), p.floor);
                    }
                    if (rnd.nextBoolean()) {
                        String cost = pick(COSTS, rnd);
                        int percent = between(20, 100, rnd);
                        add(o, cost, "데미지".equals(cost) ? percent : -percent, p.floor);
                    }
                }
                break;
        }
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
        // 층 숫자를 주면 상황에 "21층"을 그대로 쓴다 (S24 측정 20개 중 6개). 깊이는 말로만 준다
        // 신들의 이벤트는 신이 내리는 축복이다. 장소는 정하지 않는다
        sb.append("[").append(p.god.getGodName()).append("이 내리는 축복 | ").append(depth(p.floor)).append("]\n");
        sb.append("신의 성격: ").append(p.god.getTone()).append("\n");
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
                .append("규칙: 예시의 문장은 쓰지 말고 새로 지어라. 이름은 이 축복이 어떤 것인지 드러나게 15자 안팎, ")
                .append("상황은 신이 모습을 드러내거나 축복을 내미는 한두 문장, 선택지는 20자 안팎의 행동. ")
                .append("결과는 위에 정해진 변화를 이야기로 묘사하고 숫자는 쓰지 않는다. 두 선택지는 서로 달라야 한다. ")
                .append("한국어로만 쓰고 영어·한자는 쓰지 않는다.");
        return sb.toString();
    }

    private static String depth(int floor) {
        if (floor <= 15) return "던전 위층";
        if (floor <= 35) return "던전 중간층";
        return "던전 깊은 층";
    }

    /** 정해진 결과를 숫자 없는 말로. 예: "힘이 크게 오르지만 HP를 많이 잃는다" */
    static String hint(Outcome o, Monster enemy) {
        List<String> gains = new ArrayList<>();
        List<String> losses = new ArrayList<>();
        if (o.itemName != null) gains.add("'" + o.itemName + "'을(를) 얻는다");
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
        Map<String, String> found = new java.util.HashMap<>();
        if (raw != null) {
            for (String line : raw.split("\\r?\\n")) {
                Matcher m = LINE.matcher(line.trim());
                if (!m.matches()) continue;
                String key = m.group(1).replaceAll("\\s", "");
                // 굵은 글씨 표시를 지우고, 단어 사이에 생긴 긴 공백("진        한")은 한 칸으로 줄인다
                String value = m.group(2).replaceAll("\\*+", "").replaceAll("\\s+", " ").trim();
                if (!found.containsKey(key) && !value.isEmpty()) found.put(key, value);
            }
        }
        for (String key : KEYS) {
            if (!found.containsKey(key)) throw new ParseException(key + " 줄 없음");
        }
        check(found.get("이름"), "이름", MAX_NAME_LENGTH, EXAMPLE_NAME);
        check(found.get("선택지1"), "선택지1", MAX_CHOICE_LENGTH, EXAMPLE_CHOICE1);
        check(found.get("선택지2"), "선택지2", MAX_CHOICE_LENGTH, EXAMPLE_CHOICE2);
        checkText(found.get("상황"), "상황", EXAMPLE_SITUATION);
        checkText(found.get("결과1"), "결과1", EXAMPLE_RESULT1);
        checkText(found.get("결과2"), "결과2", EXAMPLE_RESULT2);
        if (normalize(found.get("선택지1")).equals(normalize(found.get("선택지2")))) {
            throw new ParseException("두 선택지가 같음");
        }
        return found;
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
