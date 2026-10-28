package com.textdungeon.ai;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;


// ══════════════════════════════════════════════
//  ChoiceLineParser 테스트
//  형식은 맞아도 게임에 붙이면 안 되는 응답(안내문·기존 선택지 베끼기, 숫자뿐인 설명)을 거르는지 확인
// ══════════════════════════════════════════════
public class ChoiceLineParserTest {

    private static final Set<String> ITEMS = new HashSet<>(Arrays.asList("item_20", "item_42"));
    private static final Set<String> FORBIDDEN = new HashSet<>(Arrays.asList("물을 마신다", "그냥 지나간다"));

    private static String response(String choice, String description) {
        return "선택지: " + choice + "\n보상설명: " + description + "\n아이템: item_20\n스탯: 힘=5, 경험치=30";
    }

    private static void assertRejected(String raw, String reason) {
        try {
            ChoiceLineParser.parse(raw, ITEMS, FORBIDDEN);
            fail("통과하면 안 됨: " + raw);
        } catch (ChoiceLineParser.ParseException e) {
            assertEquals(reason, e.getMessage());
        }
    }

    @Test
    public void 정상_응답은_통과한다() throws Exception {
        ChoiceDelta d = ChoiceLineParser.parse(
                response("샘물에 피를 한 방울 떨어뜨린다", "샘이 붉게 끓어오르며 뜨거운 기운이 팔을 타고 오른다. 힘 +5"),
                ITEMS, FORBIDDEN);
        assertEquals("샘물에 피를 한 방울 떨어뜨린다", d.getChoiceText());
        assertEquals("item_20", d.getItemId());
        assertEquals(2, d.getStats().size());
    }

    @Test
    public void 안내문을_베낀_선택지는_거른다() {
        assertRejected(response("(20자 내외의 행동)", "샘이 붉게 끓어오르며 뜨거운 기운이 오른다."), "선택지가 안내문 그대로");
        assertRejected(response("(2) 손가락 반대 방향으로 간다", "샘이 붉게 끓어오르며 뜨거운 기운이 오른다."), "선택지가 안내문 그대로");
        assertRejected(response("15자 이내의 행동", "샘이 붉게 끓어오르며 뜨거운 기운이 오른다."), "선택지가 안내문 그대로");
    }

    @Test
    public void 기존_선택지와_같으면_띄어쓰기가_달라도_거른다() {
        assertRejected(response("물을  마신다", "샘이 붉게 끓어오르며 뜨거운 기운이 오른다."), "선택지가 기존 선택지나 예시와 같음");
        assertRejected(response("그냥지나간다", "샘이 붉게 끓어오르며 뜨거운 기운이 오른다."), "선택지가 기존 선택지나 예시와 같음");
    }

    @Test
    public void 숫자뿐인_보상설명은_거른다() {
        assertRejected(response("샘물에 손을 담근다", "120"), "보상설명이 묘사 문장이 아님");
        assertRejected(response("샘물에 손을 담근다", "10 회복"), "보상설명이 묘사 문장이 아님");
        assertRejected(response("샘물에 손을 담근다", "30 피해, 데미지=3"), "보상설명이 묘사 문장이 아님");
    }

    @Test
    public void HP_변화는_데미지와_회복으로_바뀐다() throws Exception {
        ChoiceDelta d = ChoiceLineParser.parse(
                "선택지: 덫을 걷어찬다\n보상설명: 파편이 발목에 박혀 피가 흐른다. HP -12\n아이템: 없음\n스탯: 힘+2, HP-12",
                ITEMS, FORBIDDEN);
        assertEquals("데미지", d.getStats().get(1).getStatType());
        assertEquals(12, d.getStats().get(1).getValue());

        d = ChoiceLineParser.parse(
                "선택지: 샘물을 떠 마신다\n보상설명: 맑은 물이 상처를 씻어 낸다. HP +10\n아이템: 없음\n스탯: HP+10",
                ITEMS, FORBIDDEN);
        assertEquals("회복", d.getStats().get(0).getStatType());
        assertEquals(10, d.getStats().get(0).getValue());
    }

    @Test
    public void 예전_호출_방식도_그대로_동작한다() throws Exception {
        ChoiceDelta d = ChoiceLineParser.parse(
                response("물을 마신다", "맑은 물이 목을 적시며 기운이 돌아온다."), ITEMS);
        assertEquals("물을 마신다", d.getChoiceText());
    }

    @Test
    public void 주사위_검사는_예시_선택지도_거른다() {
        try {
            ChaosDice.parseResponse(
                    response(ChaosDice.EXAMPLE_CHOICE, "꺼진 심지에서 검은 연기가 피어올라 팔을 휘감는다."),
                    ITEMS, eventWithChoice());
            fail("예시를 베낀 응답이 통과함");
        } catch (ChoiceLineParser.ParseException e) {
            assertEquals("선택지가 기존 선택지나 예시와 같음", e.getMessage());
        }
    }

    private static com.textdungeon.event.GameEvent eventWithChoice() {
        com.textdungeon.event.GameEvent e = new com.textdungeon.event.GameEvent();
        e.addChoiceWithReward("물을 마신다", new ChoiceDelta("물을 마신다", "d", null, Collections.emptyList()).toReward());
        return e;
    }
}
