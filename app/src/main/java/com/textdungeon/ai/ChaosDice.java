package com.textdungeon.ai;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.textdungeon.ai.backend.LlmBackend;
import com.textdungeon.ai.backend.LlmCallback;
import com.textdungeon.ai.backend.ResponseFormat;
import com.textdungeon.ai.backend.ResponseValidator;
import com.textdungeon.event.GameEvent;
import com.textdungeon.event.ShopEvent;
import com.textdungeon.model.Item;
import com.textdungeon.model.Stat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

/**
 * 혼돈의 주사위.
 *
 * 예전 방식: 이벤트 전체를 JSON으로 보내고, AI가 이벤트 전체를 다시 써서 돌려줌 → BattleEvent로 통째 역직렬화
 * 지금 방식: AI는 "새 선택지 1개 + 보상"만 줄 단위로 출력 → 코드가 기존 이벤트 객체에 직접 붙임
 *
 * 굴리는 순서 (클라우드는 쓰지 않는다)
 *  1. 첫 굴림이면 SPECIAL_PERCENT% 확률로 결과표의 특별 선택지 (주사위 소모)
 *  2. 폰 모델이 준비돼 있으면 AI 선택지 (주사위 소모)
 *  3. 모델이 없거나 실패·시간 초과면 결과표의 일반 선택지. 첫 굴림이면 주사위를 돌려주고 한 번 더 굴릴 수 있다.
 *     다시 굴리면 그 자리를 새 결과로 바꾸고, 이때는 무엇이 나와도 주사위를 소모한다 (특별 선택지는 안 나옴)
 */
public class ChaosDice {
    private static final String TAG = "ChaosDice";
    /** 첫 굴림에서 특별 선택지가 나올 확률 (%). 다시 굴리기에서는 나오지 않는다 */
    static final int SPECIAL_PERCENT = 10;
    /** 특별 선택지 앞에 붙이는 표시 */
    static final String SPECIAL_MARK = "★ ";
    /** 프롬프트에 넣는 아이템 후보 수 */
    static final int ITEM_CANDIDATES = 5;
    // 형식을 보여 주는 예시. 모델이 이 선택지를 그대로 베끼면 실패로 처리한다
    static final String EXAMPLE_CHOICE = "제단의 촛불을 맨손으로 끈다";
    // HP 변화는 "HP-12"처럼 쓰게 하고 파서가 데미지/회복으로 바꾼다.
    // "데미지"를 적에게 준 피해로, "체력"을 HP로 쓰는 경우가 많아서 두 단어는 모델에게 보여 주지 않는다
    // 보상설명에는 수치를 쓰지 않게 한다. 실제 적용량은 코드가 끝에 붙인다 (Reward.summary)
    private static final String EXAMPLE_DESCRIPTION = "꺼진 심지에서 검은 연기가 피어올라 팔을 태우지만, 근육이 터질 듯 부풀어 오른다.";
    private static final String EXAMPLE_STATS = "힘+5, HP-12";

    /** 선택지가 어디서 왔는지 */
    public enum Source { AI, TABLE, SPECIAL }

    public interface Callback {
        /** @param refundDice 주사위를 돌려줘야 하면 true (결과표로 끝난 첫 굴림) */
        void onSuccess(GameEvent event, Source source, boolean refundDice);

        void onError(String errorMessage);
    }

    /** 한 번 굴리는 데 필요한 것. 다시 굴리기인지는 event.getChaosSlot()으로 정한다 */
    public static final class Request {
        final int floor;
        final Stat stat;
        final List<Item> nearItems;
        final List<Item> upperItems;
        final GameEvent event;
        final ChaosTable table;
        final int rewardMultiplier;
        final int expMultiplier;

        /**
         * @param nearItems        이 층대 아이템. AI 후보(5개를 고른다)와 결과표의 "floor" 아이템
         * @param upperItems       더 깊은 층의 장비. 결과표의 "upper" 아이템 (특별 선택지)
         * @param rewardMultiplier 난이도 보상 배수, expMultiplier 특성 경험치 배수.
         *                         보상설명 끝에 붙이는 요약을 실제 적용량으로 만드는 데 쓴다 (Reward.summary)
         */
        public Request(int floor, Stat stat, List<Item> nearItems, List<Item> upperItems, GameEvent event,
                       ChaosTable table, int rewardMultiplier, int expMultiplier) {
            this.floor = floor;
            this.stat = stat;
            this.nearItems = nearItems != null ? nearItems : Collections.emptyList();
            this.upperItems = upperItems != null ? upperItems : Collections.emptyList();
            this.event = event;
            this.table = table != null ? table : ChaosTable.EMPTY;
            this.rewardMultiplier = rewardMultiplier;
            this.expMultiplier = expMultiplier;
        }
    }

    /** 메인 스레드 예약. 테스트에서는 바로 실행하는 것으로 바꾼다 */
    interface MainThread {
        void post(Runnable r);

        void postDelayed(Runnable r, long delayMs);

        void remove(Runnable r);
    }

    private final LlmBackend backend;
    private final LongSupplier timeoutMs;
    private final Random random;
    private final MainThread main;

    /** @param timeoutMs 모델 응답을 기다리는 시간. 요청마다 읽는다 (디버그 플래그로 바꿀 수 있게) */
    public ChaosDice(LlmBackend backend, LongSupplier timeoutMs) {
        this(backend, timeoutMs, new Random(), handlerThread());
    }

    ChaosDice(LlmBackend backend, LongSupplier timeoutMs, Random random, MainThread main) {
        this.backend = backend;
        this.timeoutMs = timeoutMs;
        this.random = random;
        this.main = main;
    }

    private static MainThread handlerThread() {
        Handler handler = new Handler(Looper.getMainLooper());
        return new MainThread() {
            @Override
            public void post(Runnable r) {
                handler.post(r);
            }

            @Override
            public void postDelayed(Runnable r, long delayMs) {
                handler.postDelayed(r, delayMs);
            }

            @Override
            public void remove(Runnable r) {
                handler.removeCallbacks(r);
            }
        };
    }

    /** 결과표로 끝난 첫 굴림만 주사위를 돌려준다. AI·특별 선택지·다시 굴린 결과는 소모 */
    static boolean refundsDice(Source source, boolean reroll) {
        return source == Source.TABLE && !reroll;
    }

    public void roll(Request request, Callback callback) {
        GameEvent event = request.event;
        // 상점은 어떤 선택지를 눌러도 상점이 열리므로 선택지를 추가해도 의미가 없다
        if (event instanceof ShopEvent) {
            postError(callback, "상점 이벤트에는 주사위를 사용할 수 없습니다.");
            return;
        }
        if (event == null || !event.canAppendChoice()) {
            postError(callback, "선택지와 보상 개수가 맞지 않는 이벤트입니다.");
            return;
        }
        if (event.isChaosDone()) {
            postError(callback, "이 이벤트에서는 주사위를 더 쓸 수 없습니다.");
            return;
        }

        boolean reroll = event.getChaosSlot() != null;
        if (!reroll && random.nextInt(100) < SPECIAL_PERCENT) {
            ChoiceDelta special = request.table.special(event, request.floor,
                    request.nearItems, request.upperItems, random);
            if (special != null) {
                ChoiceDelta marked = special.withChoiceText(SPECIAL_MARK + special.getChoiceText());
                main.post(() -> place(request, marked, Source.SPECIAL, callback));
                return;
            }
        }

        if (!backend.isReady()) {
            Log.d(TAG, backend.name() + " 준비 안 됨 → 결과표");
            main.post(() -> placeFromTable(request, callback));
            return;
        }
        askModel(request, callback);
    }

    private void askModel(Request request, Callback callback) {
        GameEvent event = request.event;
        List<Item> candidates = pickItems(request.nearItems, random);
        Set<String> validItemIds = new HashSet<>();
        for (Item item : candidates) validItemIds.add(item.getId());

        String prompt = buildPrompt(request.floor, request.stat, candidates, event);

        // 응답과 시간 초과 중 먼저 온 쪽만 처리한다. 선택지가 두 번 붙는 것을 여기서 막는다
        AtomicBoolean finished = new AtomicBoolean(false);
        long timeout = timeoutMs.getAsLong();
        Runnable onTimeout = () -> {
            if (!finished.compareAndSet(false, true)) return;
            Log.w(TAG, backend.name() + " 시간 초과(" + timeout + "ms) → 추론을 멈추고 결과표");
            backend.cancel();
            placeFromTable(request, callback);
        };
        main.postDelayed(onTimeout, timeout);

        // 검사를 백엔드에 넘겨서, 응답이 통과하지 못하면 onError(→ 결과표)로 오게 한다
        ResponseValidator validator = text -> {
            try {
                parseResponse(text, validItemIds, event);
                return null;
            } catch (ChoiceLineParser.ParseException e) {
                Log.e(TAG, "파싱 실패(" + e.getMessage() + ") 원문:\n" + text);
                return "혼돈의 결말 해석 실패: " + e.getMessage();
            }
        };

        backend.generate(prompt, ResponseFormat.TEXT, validator, new LlmCallback() {
            @Override
            public void onResult(String text) {
                ChoiceDelta parsed;
                try {
                    parsed = parseResponse(text, validItemIds, event);
                } catch (ChoiceLineParser.ParseException e) {
                    // validator를 통과한 응답이라 여기로 오지 않는다
                    onError("혼돈의 결말 해석 실패: " + e.getMessage());
                    return;
                }
                // 파서 상한(±9999)은 형식 방어용일 뿐이라, 실제 게임 수치는 층별 상한으로 다시 자른다
                ChoiceDelta clamped = parsed.withClampedStats(request.floor);
                // 이벤트 객체는 UI가 읽고 있으므로 수정은 메인 스레드에서
                main.post(() -> {
                    if (finish(finished, onTimeout)) place(request, clamped, Source.AI, callback);
                });
            }

            @Override
            public void onError(String errorMessage) {
                main.post(() -> {
                    if (!finish(finished, onTimeout)) return;
                    Log.w(TAG, backend.name() + " 실패 → 결과표: " + errorMessage);
                    placeFromTable(request, callback);
                });
            }
        });
    }

    /** 메인 스레드에서만 부른다 */
    private void placeFromTable(Request request, Callback callback) {
        ChoiceDelta delta = request.table.normal(request.event, request.floor,
                request.nearItems, request.upperItems, random);
        if (delta == null) {
            callback.onError("혼돈의 신이 응답하지 않습니다.");
            return;
        }
        place(request, delta, Source.TABLE, callback);
    }

    /**
     * 첫 굴림이면 선택지를 붙이고, 다시 굴리기면 결과표 자리를 새 결과로 바꾼 뒤 이벤트의 주사위 상태를 남긴다.
     * 모델·결과표는 수치 없이 상황만 묘사하고, 실제로 적용되는 양(상한, 난이도·특성 배수 반영)은 코드가 붙인다.
     * 메인 스레드에서만 부른다.
     */
    private void place(Request request, ChoiceDelta delta, Source source, Callback callback) {
        GameEvent event = request.event;
        String summary = delta.toReward().summary(request.rewardMultiplier, request.expMultiplier,
                itemName(delta.getItemId(), request.nearItems, request.upperItems));
        ChoiceDelta described = summary.isEmpty() ? delta
                : delta.withDescription(delta.getRewardDescription() + " " + summary);

        Integer slot = event.getChaosSlot();
        boolean reroll = slot != null;
        try {
            if (reroll && slot < event.getChoices().size()) {
                event.replaceChoiceWithReward(slot, described.getChoiceText(), described.toReward());
            } else {
                event.addChoiceWithReward(described.getChoiceText(), described.toReward());
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "선택지 추가 실패", e);
            callback.onError(e.getMessage());
            return;
        }

        boolean refund = refundsDice(source, reroll);
        if (refund) {
            event.setChaosSlot(event.getChoices().size() - 1);
        } else {
            event.finishChaos();
        }
        Log.d(TAG, "혼돈의 주사위 " + source + (reroll ? "(다시 굴림)" : "") + ": " + described.getChoiceText());
        com.textdungeon.ai.backend.GeminiBackend.logRaw("해석된 주사위 선택지",
                "출처=" + source + " / 선택지=" + described.getChoiceText()
                        + " / 보상설명=" + described.getRewardDescription()
                        + " / 아이템=" + described.getItemId() + " / 스탯 " + described.getStats().size() + "개");
        callback.onSuccess(event, source, refund);
    }

    private static String itemName(String itemId, List<Item> nearItems, List<Item> upperItems) {
        if (itemId == null) return null;
        for (List<Item> list : Arrays.asList(nearItems, upperItems)) {
            for (Item item : list) {
                if (item != null && itemId.equals(item.getId())) return item.getName();
            }
        }
        return null;
    }

    /** 먼저 끝난 쪽이면 시간 초과 예약을 지우고 true. 메인 스레드에서만 부른다 */
    private boolean finish(AtomicBoolean finished, Runnable timeout) {
        if (!finished.compareAndSet(false, true)) {
            Log.w(TAG, "시간 초과 뒤에 도착한 응답은 버림");
            return false;
        }
        main.remove(timeout);
        return true;
    }

    private void postError(Callback callback, String message) {
        main.post(() -> callback.onError(message));
    }

    /**
     * 응답을 읽고, 기존 선택지나 프롬프트 예시를 그대로 베낀 경우는 실패로 본다.
     * 디버그 빌드의 온디바이스 측정 Activity도 같은 기준을 쓰도록 패키지 범위로 열어 둔다.
     */
    static ChoiceDelta parseResponse(String text, Set<String> validItemIds, GameEvent event)
            throws ChoiceLineParser.ParseException {
        Set<String> forbidden = new HashSet<>();
        forbidden.add(EXAMPLE_CHOICE);
        if (event.getChoices() != null) forbidden.addAll(event.getChoices());
        return ChoiceLineParser.parse(text, validItemIds, forbidden);
    }

    /** 후보 중 최대 ITEM_CANDIDATES개를 무작위로 고른다. 목록이 길면 소형 모델이 형식을 놓친다 */
    static List<Item> pickItems(List<Item> itemList, Random random) {
        List<Item> pool = new ArrayList<>();
        for (Item item : itemList) {
            if (item != null && item.getId() != null) pool.add(item);
        }
        Collections.shuffle(pool, random);
        return pool.size() > ITEM_CANDIDATES ? new ArrayList<>(pool.subList(0, ITEM_CANDIDATES)) : pool;
    }

    /**
     * 이벤트 전체 JSON 대신 필요한 정보만 짧게 넣는다 (입력 토큰도 줄어든다).
     * 형식은 "(20자 내외의 행동)" 같은 괄호 안내문 대신 채워진 예시로 보여 준다.
     * 안내문을 쓰면 소형 온디바이스 모델이 그 문구를 그대로 선택지로 베낀다 (0-A 측정에서 20건 중 19건).
     */
    static String buildPrompt(int floor, Stat stat, List<Item> itemList, GameEvent event) {
        String items = itemList.stream()
                .filter(i -> i != null && i.getId() != null)
                .map(i -> i.getId() + ":" + i.getName())
                .collect(Collectors.joining(", "));
        String choices = event.getChoices() == null ? "" : String.join(" / ", event.getChoices());

        return "[혼돈의 신 | " + floor + "층]\n"
                + "플레이어: 힘 " + stat.getStrength()
                + ", 민첩 " + stat.getAgility()
                + ", 지혜 " + stat.getWisdom()
                + ", HP " + stat.getHp() + "/" + stat.getMaxHp() + "\n"
                + "이벤트: " + event.getName() + "\n"
                + "상황: " + event.getDescription() + "\n"
                + "기존 선택지: " + choices + "\n"
                + "아이템 후보: " + (items.isEmpty() ? "없음" : items) + "\n\n"
                + "기존 선택지와 다른 새 행동 하나와 그 결과를 지어라. 결과는 무작위로 아주 좋거나 아주 나쁘게.\n"
                + "아래 예시처럼 네 줄로만 답하라.\n\n"
                + "선택지: " + EXAMPLE_CHOICE + "\n"
                + "보상설명: " + EXAMPLE_DESCRIPTION + "\n"
                + "아이템: 없음\n"
                + "스탯: " + EXAMPLE_STATS + "\n\n"
                + "규칙: 선택지는 20자 안팎의 행동. 보상설명은 결과를 묘사하는 문장이고 숫자는 쓰지 않는다. "
                + "스탯 줄은 그 묘사와 맞게, 플레이어에게 생긴 변화량만 부호를 붙여 0~3개 적는다(현재 값이 아니라 늘거나 준 양). "
                + "쓸 수 있는 것은 힘, 민첩, 지혜, 경험치, HP. 다치면 HP-수치, 회복하면 HP+수치. "
                + "적에게 준 피해는 적지 않는다. 아이템은 후보 id 하나 또는 없음.";
    }
}
