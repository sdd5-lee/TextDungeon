package com.textdungeon.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.textdungeon.ai.backend.LlmBackend;
import com.textdungeon.ai.backend.LlmCallback;
import com.textdungeon.ai.backend.ResponseFormat;
import com.textdungeon.event.GameEvent;
import com.textdungeon.model.Job;
import com.textdungeon.player.Player;

import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// ══════════════════════════════════════════════
//  ChaosDice 테스트: 굴리는 순서와 주사위 반환
//  첫 굴림: 10% 특별(소모) → 모델(소모) → 결과표(반환, 다시 굴리기 가능)
//  다시 굴리기: 같은 자리를 바꾸고 무엇이 나와도 소모, 특별은 안 나옴
// ══════════════════════════════════════════════
public class ChaosDiceTest {

    private static final String TABLE = "{\"types\":{\"normal\":{\"choices\":["
            + entry("표 선택지 하나") + "," + entry("표 선택지 둘") + "],"
            + "\"specials\":[{\"choice\":\"특별한 길\",\"text\":\"특별한 일이 일어난다.\",\"stats\":{\"힘\":200}}]}}}";

    private static final String MODEL_ANSWER =
            "선택지: 샘물에 피를 떨어뜨린다\n보상설명: 샘이 붉게 끓어오른다.\n아이템: 없음\n스탯: 힘+3";

    private static String entry(String choice) {
        return "{\"choice\":\"" + choice + "\","
                + "\"good\":{\"text\":\"좋은 일이 생긴다.\",\"stats\":{\"힘\":50}},"
                + "\"bad\":{\"text\":\"나쁜 일이 생긴다.\",\"stats\":{\"데미지\":50}}}";
    }

    /** 특별 선택지 확률(nextInt(100))만 정해 둔 값으로. 목록에서는 첫 번째, 결과는 좋은 쪽 */
    private static class FixedRandom extends Random {
        int specialRoll = 99;

        @Override
        public int nextInt(int bound) {
            return bound == 100 ? specialRoll : 0;
        }

        @Override
        public boolean nextBoolean() {
            return true;
        }
    }

    /** post는 바로 실행하고, postDelayed(시간 초과)는 모아 두었다가 fireTimeouts()로 실행 */
    private static class ImmediateMain implements ChaosDice.MainThread {
        final List<Runnable> delayed = new ArrayList<>();

        @Override
        public void post(Runnable r) {
            r.run();
        }

        @Override
        public void postDelayed(Runnable r, long delayMs) {
            delayed.add(r);
        }

        @Override
        public void remove(Runnable r) {
            delayed.remove(r);
        }

        void fireTimeouts() {
            List<Runnable> copy = new ArrayList<>(delayed);
            delayed.clear();
            for (Runnable r : copy) r.run();
        }
    }

    private static class FakeModel implements LlmBackend {
        boolean ready;
        boolean cancelled;
        LlmCallback pending;

        @Override
        public void generate(String prompt, ResponseFormat format, LlmCallback callback) {
            pending = callback;
        }

        @Override
        public void cancel() {
            cancelled = true;
        }

        @Override
        public boolean isReady() {
            return ready;
        }

        @Override
        public String name() {
            return "가짜 모델";
        }
    }

    private static class Result implements ChaosDice.Callback {
        ChaosDice.Source source;
        Boolean refund;
        String error;
        int calls;

        @Override
        public void onSuccess(GameEvent event, ChaosDice.Source source, boolean refundDice) {
            this.source = source;
            this.refund = refundDice;
            calls++;
        }

        @Override
        public void onError(String errorMessage) {
            error = errorMessage;
            calls++;
        }
    }

    private GameEvent event;
    private FakeModel model;
    private FixedRandom random;
    private ImmediateMain main;
    private ChaosDice dice;
    private ChaosTable table;

    @Before
    public void setUp() {
        event = new Gson().fromJson("{\"id\":\"event_x\",\"name\":\"시험\",\"description\":\"시험 이벤트\","
                + "\"choices\":[\"물을 마신다\",\"그냥 지나간다\"],\"rewards\":[{},{}]}", GameEvent.class);
        table = ChaosTable.parse(new StringReader(TABLE));
        model = new FakeModel();
        random = new FixedRandom();
        main = new ImmediateMain();
        dice = new ChaosDice(model, () -> 8_000, random, main);
    }

    private Result roll() {
        Result r = new Result();
        dice.roll(new ChaosDice.Request(10, new Player("테스터", Job.WARRIOR).getStat(),
                Collections.emptyList(), Collections.emptyList(), event, table, 1, 1), r);
        return r;
    }

    @Test
    public void 모델이_없으면_결과표_선택지를_붙이고_주사위를_돌려준다() {
        Result r = roll();

        assertEquals(ChaosDice.Source.TABLE, r.source);
        assertTrue(r.refund);
        assertEquals(3, event.getChoices().size());
        assertEquals("표 선택지 하나", event.getChoices().get(2));
        assertEquals(Integer.valueOf(2), event.getChaosSlot());
        assertFalse(event.isChaosDone());
    }

    @Test
    public void 다시_굴리면_같은_자리를_바꾸고_주사위를_소모한다() {
        roll();
        Result again = roll();

        assertEquals(ChaosDice.Source.TABLE, again.source);
        assertFalse(again.refund);
        assertEquals(3, event.getChoices().size());
        assertEquals(3, event.getRewards().size());
        assertEquals("표 선택지 둘", event.getChoices().get(2)); // 방금 나온 문장은 다시 고르지 않는다
        assertTrue(event.isChaosDone());
        assertNull(event.getChaosSlot());
    }

    @Test
    public void 다시_굴리기에서는_특별_선택지가_나오지_않는다() {
        roll();
        random.specialRoll = 0;
        Result again = roll();

        assertEquals(ChaosDice.Source.TABLE, again.source);
        assertFalse(again.refund);
    }

    @Test
    public void 첫_굴림_10퍼센트_안이면_특별_선택지이고_주사위를_소모한다() {
        random.specialRoll = ChaosDice.SPECIAL_PERCENT - 1;
        model.ready = true;
        Result r = roll();

        assertEquals(ChaosDice.Source.SPECIAL, r.source);
        assertFalse(r.refund);
        assertNull(model.pending); // 모델에 묻지 않는다
        assertEquals(ChaosDice.SPECIAL_MARK + "특별한 길", event.getChoices().get(2));
        // 10층 힘 상한 15의 200%. 보상설명 끝에 실제 적용량이 붙는다
        assertEquals("특별한 일이 일어난다. (힘 +30)", event.getRewards().get(2).getDescription());
        assertTrue(event.isChaosDone());
    }

    @Test
    public void 확률_경계_10이면_특별이_아니다() {
        random.specialRoll = ChaosDice.SPECIAL_PERCENT;
        assertEquals(ChaosDice.Source.TABLE, roll().source);
    }

    @Test
    public void 모델이_답하면_AI_선택지이고_주사위를_소모한다() {
        model.ready = true;
        Result r = roll();
        assertNotNull(model.pending);
        model.pending.onResult(MODEL_ANSWER);

        assertEquals(ChaosDice.Source.AI, r.source);
        assertFalse(r.refund);
        assertEquals("샘물에 피를 떨어뜨린다", event.getChoices().get(2));
        assertTrue(event.isChaosDone());
        assertTrue(main.delayed.isEmpty()); // 시간 초과 예약은 지운다
    }

    @Test
    public void 모델이_실패하면_결과표로_넘어가고_주사위를_돌려준다() {
        model.ready = true;
        Result r = roll();
        model.pending.onError("추론 실패");

        assertEquals(ChaosDice.Source.TABLE, r.source);
        assertTrue(r.refund);
    }

    @Test
    public void 형식이_틀린_응답도_결과표로() {
        model.ready = true;
        Result r = roll();
        model.pending.onResult("아무 말");

        assertEquals(ChaosDice.Source.TABLE, r.source);
    }

    @Test
    public void 시간이_넘으면_추론을_멈추고_결과표_늦은_응답은_버린다() {
        model.ready = true;
        Result r = roll();
        LlmCallback late = model.pending;
        main.fireTimeouts();

        assertTrue(model.cancelled);
        assertEquals(ChaosDice.Source.TABLE, r.source);

        late.onResult(MODEL_ANSWER);
        assertEquals(1, r.calls);
        assertEquals(3, event.getChoices().size());
    }

    @Test
    public void 결과표도_비고_모델도_없으면_오류() {
        table = ChaosTable.EMPTY;
        Result r = roll();

        assertNotNull(r.error);
        assertEquals(2, event.getChoices().size());
    }

    @Test
    public void 끝난_이벤트에는_굴리지_않는다() {
        event.finishChaos();
        Result r = roll();

        assertNotNull(r.error);
        assertEquals(2, event.getChoices().size());
    }
}
