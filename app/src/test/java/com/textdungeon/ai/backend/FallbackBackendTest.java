package com.textdungeon.ai.backend;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;


// ══════════════════════════════════════════════
//  FallbackBackend 테스트
//  온디바이스(primary)가 준비 안 됨 / 실패 / 검사 불통과 / 시간 초과일 때 클라우드(secondary)로 넘기고,
//  바깥 콜백은 한 번만 불리는지 확인
// ══════════════════════════════════════════════
public class FallbackBackendTest {

    private static final long TIMEOUT_MS = 100;
    private static final ResponseValidator NOT_BAD = text -> text.equals("bad") ? "검사 불통과" : null;

    /** 응답을 직접 넣어 주는 가짜 백엔드 */
    private static class FakeBackend implements LlmBackend {
        final String name;
        boolean ready = true;
        int calls;
        boolean cancelled;
        LlmCallback last;
        String answer; // null이면 응답하지 않고 기다림 (last로 나중에 응답)

        FakeBackend(String name, String answer) {
            this.name = name;
            this.answer = answer;
        }

        @Override
        public void generate(String prompt, ResponseFormat format, LlmCallback callback) {
            calls++;
            last = callback;
            if (answer != null) {
                if (answer.startsWith("ERROR")) callback.onError(answer);
                else callback.onResult(answer);
            }
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
            return name;
        }
    }

    private static class Recorder implements LlmCallback {
        final List<String> events = Collections.synchronizedList(new ArrayList<>());
        final CountDownLatch first = new CountDownLatch(1);

        @Override
        public void onResult(String text) {
            events.add("result:" + text);
            first.countDown();
        }

        @Override
        public void onError(String errorMessage) {
            events.add("error:" + errorMessage);
            first.countDown();
        }

        void await() throws InterruptedException {
            assertTrue("콜백이 오지 않음", first.await(2, TimeUnit.SECONDS));
        }
    }

    private static FallbackBackend fallback(FakeBackend primary, FakeBackend secondary) {
        return new FallbackBackend(primary, secondary, () -> TIMEOUT_MS);
    }

    @Test
    public void 온디바이스가_응답하면_클라우드는_부르지_않는다() throws Exception {
        FakeBackend device = new FakeBackend("device", "good");
        FakeBackend cloud = new FakeBackend("cloud", "cloud");
        Recorder r = new Recorder();

        fallback(device, cloud).generate("p", ResponseFormat.TEXT, NOT_BAD, r);
        r.await();
        Thread.sleep(TIMEOUT_MS * 2); // 타임아웃이 지나도 클라우드로 넘어가지 않아야 함

        assertEquals(Collections.singletonList("result:good"), r.events);
        assertEquals(0, cloud.calls);
        assertFalse(device.cancelled);
    }

    @Test
    public void 온디바이스가_준비_안_됐으면_바로_클라우드() throws Exception {
        FakeBackend device = new FakeBackend("device", "good");
        device.ready = false;
        FakeBackend cloud = new FakeBackend("cloud", "cloud");
        Recorder r = new Recorder();

        fallback(device, cloud).generate("p", ResponseFormat.TEXT, NOT_BAD, r);
        r.await();

        assertEquals(Collections.singletonList("result:cloud"), r.events);
        assertEquals(0, device.calls);
    }

    @Test
    public void 온디바이스_오류면_클라우드로_넘긴다() throws Exception {
        FakeBackend device = new FakeBackend("device", "ERROR 엔진");
        FakeBackend cloud = new FakeBackend("cloud", "cloud");
        Recorder r = new Recorder();

        fallback(device, cloud).generate("p", ResponseFormat.TEXT, NOT_BAD, r);
        r.await();

        assertEquals(Collections.singletonList("result:cloud"), r.events);
    }

    @Test
    public void 온디바이스_응답이_검사를_통과하지_못하면_클라우드로_넘긴다() throws Exception {
        FakeBackend device = new FakeBackend("device", "bad");
        FakeBackend cloud = new FakeBackend("cloud", "cloud");
        Recorder r = new Recorder();

        fallback(device, cloud).generate("p", ResponseFormat.TEXT, NOT_BAD, r);
        r.await();

        assertEquals(Collections.singletonList("result:cloud"), r.events);
        assertEquals(1, cloud.calls);
    }

    @Test
    public void 둘_다_검사를_통과하지_못하면_오류_한_번() throws Exception {
        FakeBackend device = new FakeBackend("device", "bad");
        FakeBackend cloud = new FakeBackend("cloud", "bad");
        Recorder r = new Recorder();

        fallback(device, cloud).generate("p", ResponseFormat.TEXT, NOT_BAD, r);
        r.await();

        assertEquals(Collections.singletonList("error:검사 불통과"), r.events);
    }

    @Test
    public void 시간_초과면_추론을_멈추고_클라우드로_넘기고_늦은_응답은_버린다() throws Exception {
        FakeBackend device = new FakeBackend("device", null); // 응답하지 않음
        FakeBackend cloud = new FakeBackend("cloud", "cloud");
        Recorder r = new Recorder();

        fallback(device, cloud).generate("p", ResponseFormat.TEXT, NOT_BAD, r);
        r.await();
        assertTrue("시간 초과 때 온디바이스 추론을 멈춰야 함", device.cancelled);

        device.last.onResult("good"); // 시간 초과 뒤에 도착한 온디바이스 응답
        device.last.onError("ERROR 늦은 오류");

        assertEquals(Collections.singletonList("result:cloud"), r.events);
        assertEquals(1, cloud.calls);
    }

    @Test
    public void 시간_초과_뒤_클라우드가_실패해도_콜백은_한_번() throws Exception {
        FakeBackend device = new FakeBackend("device", null);
        FakeBackend cloud = new FakeBackend("cloud", "ERROR HTTP 401");
        Recorder r = new Recorder();

        fallback(device, cloud).generate("p", ResponseFormat.TEXT, NOT_BAD, r);
        r.await();
        device.last.onResult("good");

        assertEquals(Collections.singletonList("error:ERROR HTTP 401"), r.events);
    }

    @Test
    public void 바깥에서_멈춘_요청은_클라우드로_넘기지_않는다() throws Exception {
        FakeBackend device = new FakeBackend("device", null);
        FakeBackend cloud = new FakeBackend("cloud", "cloud");
        Recorder r = new Recorder();
        FallbackBackend backend = new FallbackBackend(device, cloud, () -> 10_000);

        backend.generate("p", ResponseFormat.TEXT, NOT_BAD, r);
        backend.cancel();                    // ChaosDice 전체 시간 초과
        device.last.onError("ERROR 멈춤");    // 멈춘 온디바이스가 오류로 끝남
        r.await();

        assertTrue(device.cancelled);
        assertEquals(0, cloud.calls);
        assertEquals(Collections.singletonList("error:ERROR 멈춤"), r.events);
    }

    @Test
    public void 한번만_콜백은_두번째_호출을_버린다() {
        Recorder r = new Recorder();
        OnceCallback once = new OnceCallback(r);
        once.onResult("a");
        once.onResult("b");
        once.onError("c");
        assertEquals(Collections.singletonList("result:a"), r.events);
    }
}
