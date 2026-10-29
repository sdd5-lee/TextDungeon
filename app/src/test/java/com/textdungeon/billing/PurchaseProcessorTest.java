package com.textdungeon.billing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// ══════════════════════════════════════════════
//  PurchaseProcessor 테스트
//  저장이 성공해야만 확인(acknowledge)하고, 회수는 동기화에서만 한다
// ══════════════════════════════════════════════
public class PurchaseProcessorTest {

    private EntitlementStore store;
    private BillingGateway gateway;
    private PurchaseProcessor processor;
    private Set<String> saved;

    @Before
    public void setUp() {
        store = mock(EntitlementStore.class);
        gateway = mock(BillingGateway.class);
        saved = new HashSet<>();
        // 저장소는 마지막으로 저장한 값을 돌려준다
        when(store.loadOwned()).thenAnswer(inv -> new HashSet<>(saved));
        when(store.saveOwned(any())).thenAnswer(inv -> {
            saved = new HashSet<>(inv.<Set<String>>getArgument(0));
            return true;
        });
        processor = new PurchaseProcessor(store, gateway);
    }

    private static PurchaseInfo purchase(PurchaseInfo.State state, boolean acknowledged, String... ids) {
        return new PurchaseInfo(Arrays.asList(ids), "token-" + String.join("-", ids), state, acknowledged);
    }

    @Test
    public void 확인_안_된_구매는_저장한_뒤_한_번_확인한다() {
        PurchaseInfo p = purchase(PurchaseInfo.State.PURCHASED, false, "remove_ads");

        assertEquals(PurchaseProcessor.Outcome.GRANTED, processor.process(p));

        InOrder order = inOrder(store, gateway);
        order.verify(store).saveOwned(any());
        order.verify(gateway).acknowledge(eq("token-remove_ads"), any());
        verify(gateway, times(1)).acknowledge(anyString(), any());
        assertTrue(saved.contains("remove_ads"));
    }

    @Test
    public void 이미_보유하고_확인된_구매는_저장도_확인도_안_한다() {
        saved.add("gem_boost_1");
        PurchaseInfo p = purchase(PurchaseInfo.State.PURCHASED, true, "gem_boost_1");

        assertEquals(PurchaseProcessor.Outcome.ALREADY_OWNED, processor.process(p));
        verify(store, never()).saveOwned(any());
        verify(gateway, never()).acknowledge(anyString(), any());
    }

    @Test
    public void 결제_대기는_지급도_확인도_안_한다() {
        PurchaseInfo p = purchase(PurchaseInfo.State.PENDING, false, "gem_boost_1");

        assertEquals(PurchaseProcessor.Outcome.PENDING, processor.process(p));
        verify(store, never()).saveOwned(any());
        verify(gateway, never()).acknowledge(anyString(), any());
    }

    @Test
    public void 같은_구매가_두_번_들어오면_두_번째는_저장하지_않는다() {
        PurchaseInfo p = purchase(PurchaseInfo.State.PURCHASED, false, "gem_boost_1");

        assertEquals(PurchaseProcessor.Outcome.GRANTED, processor.process(p));
        assertEquals(PurchaseProcessor.Outcome.ALREADY_OWNED, processor.process(p));
        verify(store, times(1)).saveOwned(any());
    }

    @Test
    public void 모르는_상품은_확인하지_않는다() {
        PurchaseInfo p = purchase(PurchaseInfo.State.PURCHASED, false, "unknown_item");

        assertEquals(PurchaseProcessor.Outcome.UNKNOWN_PRODUCT, processor.process(p));
        verify(gateway, never()).acknowledge(anyString(), any());
    }

    @Test
    public void 저장에_실패하면_확인하지_않는다() {
        doReturn(false).when(store).saveOwned(any());
        PurchaseInfo p = purchase(PurchaseInfo.State.PURCHASED, false, "remove_ads");

        assertEquals(PurchaseProcessor.Outcome.SAVE_FAILED, processor.process(p));
        verify(gateway, never()).acknowledge(anyString(), any());
    }

    @Test
    public void 동기화_캐시에_있고_Play에_없으면_회수() {
        saved.addAll(Arrays.asList("remove_ads", "gem_boost_1"));
        List<PurchaseInfo> play = Collections.singletonList(
                purchase(PurchaseInfo.State.PURCHASED, true, "gem_boost_1"));

        PurchaseProcessor.SyncResult r = processor.syncOwned(play);

        assertTrue(r.saved);
        assertEquals(Collections.singleton("remove_ads"), r.revoked);
        assertTrue(r.added.isEmpty());
        assertEquals(Collections.singleton("gem_boost_1"), saved);
    }

    @Test
    public void 동기화_Play에_새로_있으면_추가하고_확인한다() {
        List<PurchaseInfo> play = Collections.singletonList(
                purchase(PurchaseInfo.State.PURCHASED, false, "gem_boost_1"));

        PurchaseProcessor.SyncResult r = processor.syncOwned(play);

        assertEquals(Collections.singleton("gem_boost_1"), r.added);
        InOrder order = inOrder(store, gateway);
        order.verify(store).saveOwned(any());
        order.verify(gateway).acknowledge(eq("token-gem_boost_1"), any());
    }

    @Test
    public void 동기화_결제_대기는_보유로_치지_않는다() {
        List<PurchaseInfo> play = Collections.singletonList(
                purchase(PurchaseInfo.State.PENDING, false, "remove_ads"));

        PurchaseProcessor.SyncResult r = processor.syncOwned(play);

        assertFalse(saved.contains("remove_ads"));
        assertTrue(r.added.isEmpty());
        verify(gateway, never()).acknowledge(anyString(), any());
    }

    @Test
    public void 동기화_저장에_실패하면_캐시도_확인도_그대로() {
        saved.add("remove_ads");
        doReturn(false).when(store).saveOwned(any());

        PurchaseProcessor.SyncResult r = processor.syncOwned(Collections.singletonList(
                purchase(PurchaseInfo.State.PURCHASED, false, "gem_boost_1")));

        assertFalse(r.saved);
        assertFalse(r.changed());
        assertEquals(Collections.singleton("remove_ads"), saved);
        verify(gateway, never()).acknowledge(anyString(), any());
    }
}
