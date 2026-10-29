package com.textdungeon.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.gson.Gson;
import com.textdungeon.ai.backend.LlmBackend;
import com.textdungeon.ai.backend.LlmCallback;
import com.textdungeon.ai.backend.ResponseFormat;
import com.textdungeon.event.BattleEvent;
import com.textdungeon.event.GameEvent;
import com.textdungeon.model.Item;
import com.textdungeon.model.Monster;
import com.textdungeon.model.Reward;
import com.textdungeon.model.RewardStat;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

// ══════════════════════════════════════════════
//  TemplateEventGenerator 테스트
//  뼈대(종류·몬스터·보상)는 코드가 정하고, AI 응답 여섯 줄은 검사해서 묘사로만 쓴다
// ══════════════════════════════════════════════
public class TemplateEventGeneratorTest {

    private static final String ANSWER = "이름: 피 묻은 제단의 시험\n"
            + "상황: 붉게 물든 제단 위에서 낡은 칼 한 자루가 저절로 떨고 있다.\n"
            + "선택지1: 칼을 뽑아 제단에 꽂는다\n"
            + "결과1: 칼이 손을 베고 피를 마시자, 몸속에서 뜨거운 힘이 솟구친다.\n"
            + "선택지2: 제단에 고개 숙이고 물러난다\n"
            + "결과2: 제단의 빛이 잦아들고, 바닥에 떨어진 작은 주머니 하나가 남는다.";

    private static final Gson GSON = new Gson();

    private static List<Item> items(String... ids) {
        List<Item> list = new ArrayList<>();
        for (String id : ids) {
            list.add(Item.createFromJson("{\"id\":\"" + id + "\",\"name\":\"" + id + " 이름\",\"type\":\"weapon\"}"));
        }
        return list;
    }

    private static List<Monster> monsters() {
        return Arrays.asList(GSON.fromJson("{\"id\":\"mon_1\",\"name\":\"해골 기사\"}", Monster.class));
    }

    private static TemplateEventGenerator.Plan plan(AiType god) {
        return TemplateEventGenerator.plan(god, 21, items("near_1", "near_2"), items("deep_1", "deep_2", "deep_3"),
                monsters(), new Random(7));
    }

    private static int stat(TemplateEventGenerator.Outcome o, String type) {
        for (RewardStat s : o.stats) if (type.equals(s.getStatType())) return s.getValue();
        return 0;
    }

    @Test
    public void 투쟁의_신은_층_몬스터와_전투하고_이기면_스탯_둘() {
        TemplateEventGenerator.Plan p = plan(AiType.STRUGGLE);

        assertEquals("battle", p.type);
        assertEquals("mon_1", p.enemy.getId());
        assertEquals("battle", p.outcomes.get(0).action);
        assertEquals(2, p.outcomes.get(0).stats.size());
        assertEquals("escape", p.outcomes.get(1).action);
        assertTrue(p.outcomes.get(1).stats.isEmpty());
    }

    @Test
    public void 보물의_신은_서로_다른_심층_장비_둘() {
        TemplateEventGenerator.Plan p = plan(AiType.TREASURE);

        String a = p.outcomes.get(0).itemId;
        String b = p.outcomes.get(1).itemId;
        assertTrue(a.startsWith("deep_"));
        assertTrue(b.startsWith("deep_"));
        assertNotEquals(a, b);
        assertTrue(p.outcomes.get(0).stats.isEmpty());
    }

    @Test
    public void 모험의_신은_큰_위험_큰_보상과_작은_위험_작은_보상() {
        TemplateEventGenerator.Plan p = plan(AiType.ADVENTURE);

        int bigDamage = stat(p.outcomes.get(0), "데미지");
        int smallDamage = stat(p.outcomes.get(1), "데미지");
        assertTrue(bigDamage > smallDamage);
        // 21층 데미지 상한 83의 70% 이상
        assertTrue(bigDamage >= 58);
    }

    @Test
    public void 보상_수치는_층별_상한을_넘지_않는다() {
        for (AiType god : AiType.values()) {
            for (int seed = 0; seed < 30; seed++) {
                TemplateEventGenerator.Plan p = TemplateEventGenerator.plan(god, 41, items("n"), items("d1", "d2"),
                        monsters(), new Random(seed));
                for (TemplateEventGenerator.Outcome o : p.outcomes) {
                    for (RewardStat s : o.stats) {
                        assertEquals(god + " " + s.getStatType(),
                                RewardLimits.clamp(s.getStatType(), s.getValue(), 41), s.getValue());
                    }
                }
            }
        }
    }

    @Test
    public void 프롬프트에는_정해진_결과가_숫자_없이_들어간다() {
        TemplateEventGenerator.Plan p = plan(AiType.STRUGGLE);
        String prompt = TemplateEventGenerator.prompt(p);

        assertTrue(prompt.contains("등장하는 적: 해골 기사"));
        assertTrue(prompt.contains("(전투) 해골 기사과(와) 싸운다. 이기면"));
        assertTrue(prompt.contains("(도망)"));
        String hints = prompt.substring(prompt.indexOf("정해진 결과:"), prompt.indexOf("이 결과에 맞는"));
        assertFalse(hints, hints.replaceAll("\\d+\\.", "").matches("(?s).*\\d.*"));
    }

    @Test
    public void 프롬프트는_신의_축복이고_층_숫자_대신_깊이를_준다() {
        TemplateEventGenerator.Plan p = plan(AiType.DEATH);
        String prompt = TemplateEventGenerator.prompt(p);

        assertTrue(prompt, prompt.startsWith("[죽음의 신이 내리는 축복 | 던전 중간층]"));
        assertFalse(prompt, prompt.contains("21층"));
        assertFalse(prompt, prompt.contains("장소"));
    }

    @Test
    public void 묘사에_층수는_있어도_받는다() throws Exception {
        String answer = ANSWER.replace("붉게 물든 제단", "21층 깊은 곳의 붉게 물든 제단");
        assertTrue(TemplateEventGenerator.parseLines(answer).get("상황").startsWith("21층"));
        // HP는 게임에서 쓰는 말이라 받는다
        assertNotNull(TemplateEventGenerator.parseLines(ANSWER.replace("피를 마시자", "HP를 마시자")));
    }

    @Test
    public void 응답_여섯_줄을_이벤트로_바꾸고_실제_적용량을_붙인다() throws Exception {
        TemplateEventGenerator.Plan p = plan(AiType.STRUGGLE);
        GameEvent e = new TemplateEventGenerator(null).toEvent(ANSWER, p, 2, 1);

        assertTrue(e instanceof BattleEvent);
        assertEquals("mon_1", ((BattleEvent) e).getEnemyId());
        assertEquals("피 묻은 제단의 시험", e.getName());
        assertEquals(Arrays.asList("칼을 뽑아 제단에 꽂는다", "제단에 고개 숙이고 물러난다"), e.getChoices());
        Reward win = e.getRewards().get(0);
        assertEquals("battle", win.getAction());
        assertTrue(win.getDescription(), win.getDescription().startsWith("칼이 손을 베고"));
        assertTrue(win.getDescription(), win.getDescription().endsWith(")")); // (힘 +N, 체력 +N)
        assertEquals("escape", e.getRewards().get(1).getAction());
        assertEquals(21, e.getMinFloor());
    }

    @Test
    public void 보물_이벤트는_아이템을_보상에_넣는다() throws Exception {
        TemplateEventGenerator.Plan p = plan(AiType.TREASURE);
        GameEvent e = new TemplateEventGenerator(null).toEvent(ANSWER, p, 1, 1);

        assertFalse(e instanceof BattleEvent);
        assertEquals(p.outcomes.get(0).itemId, e.getRewards().get(0).getItemId());
        assertNull(e.getRewards().get(0).getAction());
        assertTrue(e.getRewards().get(0).getDescription().endsWith("(" + p.outcomes.get(0).itemName + ")"));
    }

    @Test
    public void 굵은_글씨나_띄어쓰기가_달라도_읽는다() throws Exception {
        String answer = ANSWER.replace("이름:", "**이름**:").replace("선택지1:", "선택지 1 :").replace("결과2:", "- 결과2：");
        assertEquals("피 묻은 제단의 시험", TemplateEventGenerator.parseLines(answer).get("이름"));
        assertNotNull(TemplateEventGenerator.parseLines(answer).get("결과2"));
        String spaced = ANSWER.replace("붉게 물든", "붉게        물든");
        assertTrue(TemplateEventGenerator.parseLines(spaced).get("상황").startsWith("붉게 물든 제단"));
    }

    @Test
    public void 규칙을_어긴_응답은_버린다() {
        rejects(ANSWER.replace("선택지2: 제단에 고개 숙이고 물러난다\n", ""), "선택지2 줄 없음");
        rejects(ANSWER.replace("뜨거운 힘이", "힘 5가"), "숫자");
        rejects(ANSWER.replace("피 묻은 제단의 시험", TemplateEventGenerator.EXAMPLE_NAME), "예시");
        rejects(ANSWER.replace("제단에 고개 숙이고 물러난다", "칼을 뽑아 제단에 꽂는다"), "같음");
        rejects(ANSWER.replace("피 묻은 제단의 시험", "(15자 안팎의 이름)"), "안내문");
        rejects(ANSWER.replace("칼을 뽑아 제단에 꽂는다", "칼을 뽑아 들고 제단 위로 올라가 하늘을 향해 크게 외친다"), "넘음");
        rejects(ANSWER.replace("뜨거운 힘이", "hardening된 힘이"), "외국 글자");
        rejects(ANSWER.replace("작은 주머니", "작은 微주머니"), "외국 글자");
        rejects(ANSWER.replace("제단에 고개", "제단Ни에 고개"), "외국 글자");
        rejects(ANSWER.replace("제단에 고개", "제단वश에 고개"), "외국 글자");
        rejects(ANSWER.replace("붉게 물든", "붉게_물든"), "외국 글자");
    }

    private static void rejects(String answer, String reason) {
        try {
            TemplateEventGenerator.parseLines(answer);
            fail("통과하면 안 됨: " + reason);
        } catch (TemplateEventGenerator.ParseException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(reason));
        }
    }

    @Test
    public void 백엔드에는_틀_방식_출력_상한과_온도로_보낸다() {
        int[] maxTokens = {0};
        Double[] temperature = {null};
        LlmBackend backend = new LlmBackend() {
            @Override
            public void generate(String prompt, ResponseFormat format, LlmCallback callback) {
                fail("출력 상한 없이 부르면 안 됨");
            }

            @Override
            public void generate(String prompt, ResponseFormat format, int max, Double temp, LlmCallback callback) {
                maxTokens[0] = max;
                temperature[0] = temp;
                callback.onResult(ANSWER);
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public String name() {
                return "가짜";
            }
        };
        GameEvent[] made = new GameEvent[1];
        new TemplateEventGenerator(backend, new Random(1)).generate(1, AiType.DEATH, items("n"), items("d1", "d2"),
                monsters(), 1, 1, new AiCallback() {
                    @Override
                    public void onSuccess(GameEvent e) {
                        made[0] = e;
                    }

                    @Override
                    public void onError(String errorMessage) {
                        fail(errorMessage);
                    }
                });
        assertEquals(TemplateEventGenerator.MAX_OUTPUT_TOKENS, maxTokens[0]);
        assertEquals(TemplateEventGenerator.TEMPERATURE, temperature[0], 0.0);
        assertEquals("event_death_1", made[0].getId());
    }
}
