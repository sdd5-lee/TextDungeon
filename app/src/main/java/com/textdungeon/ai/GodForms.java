package com.textdungeon.ai;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 신이 축복을 내밀 때 드러나는 형상. 신마다 11가지를 하급·중급·상급으로 나눠 33개다.
 * 등급이 보상의 세기와 아이템 등급을 정한다 (TemplateEventGenerator.plan).
 * 플레이어가 이름만 보고 세기를 짐작할 수 있게 하급은 흔한 것, 중급은 귀한 재료, 상급은 전설 속 존재의 것이다.
 * 장소도 형상도 없이 만들면 작은 모델의 글이 흔들려(S24, 20개 중 8~11개 성공) 구체적인 소재를 준다.
 */
final class GodForms {

    enum Grade {
        LOW("소박한 축복", 25, 45, 50),
        MID("값진 축복", 50, 70, 35),
        HIGH("귀한 축복", 80, 100, 15);

        /** 프롬프트에 형상과 함께 주는 말 */
        final String label;
        /** 얻는 쪽 보상(스탯, 골드, 경험치)을 층 상한의 몇 %로 줄지 */
        final int minPercent;
        final int maxPercent;
        /** 뽑힐 확률 (%) */
        final int weight;

        Grade(String label, int minPercent, int maxPercent, int weight) {
            this.label = label;
            this.minPercent = minPercent;
            this.maxPercent = maxPercent;
            this.weight = weight;
        }
    }

    static final class Form {
        final String name;
        final Grade grade;

        Form(String name, Grade grade) {
            this.name = name;
            this.grade = grade;
        }
    }

    // 한 줄이 한 가지 형상의 하급, 중급, 상급
    private static final Map<AiType, String[][]> FORMS = new EnumMap<>(AiType.class);

    static {
        FORMS.put(AiType.ADVENTURE, new String[][]{
                {"이름 모를 몬스터의 깃털", "황금 깃털", "가루다의 깃털"},
                {"찢어진 낡은 지도", "은실로 수놓은 지도", "세계수 잎맥의 지도"},
                {"녹슨 나침반", "별빛 나침반", "바람신의 나침반"},
                {"해진 가죽 장화", "은빛 날개 장화", "천리마의 말굽"},
                {"꺼져 가는 등불", "푸른 불꽃 등불", "길잡이별의 등불"},
                {"삭은 밧줄", "은빛 밧줄", "거인의 머리카락 밧줄"},
                {"녹슨 열쇠", "수정 열쇠", "천 개의 문을 여는 열쇠"},
                {"깨진 소라껍데기", "진주 소라", "바다 용왕의 소라"},
                {"스치는 산들바람", "은빛 회오리", "폭풍 거인의 숨결"},
                {"희미한 발자국", "빛나는 발자국", "첫 모험가의 발자국"},
                {"해진 돛 조각", "은빛 돛", "유령선의 검은 돛"},
        });
        FORMS.put(AiType.CHAOS, new String[][]{
                {"금 간 손거울", "일그러진 은거울", "끝없이 비추는 거울"},
                {"삐걱대는 팽이", "멈추지 않는 팽이", "세계를 돌리는 팽이"},
                {"찢어진 종이 가면", "웃다 우는 가면", "천 개의 얼굴 가면"},
                {"꼬리 없는 들고양이", "웃는 보랏빛 고양이", "사라지는 거대 고양이"},
                {"멈춘 회중시계", "거꾸로 가는 시계", "시간을 삼킨 시계"},
                {"터지는 비눗방울", "무지개 거품", "세상을 담은 거품"},
                {"줄 끊긴 꼭두각시", "혼자 춤추는 인형", "미친 왕의 광대"},
                {"네모난 구름 조각", "노래하는 구름", "하늘을 뒤집는 구름"},
                {"엉킨 실타래", "스스로 풀리는 실타래", "끝이 없는 실타래"},
                {"금 간 알", "무늬가 바뀌는 알", "혼돈을 품은 알"},
                {"벽에 그려진 문", "공중에 뜬 문", "어디로든 열리는 문"},
        });
        FORMS.put(AiType.DEATH, new String[][]{
                {"금 간 해골", "은을 입힌 해골", "거인 왕의 해골"},
                {"꺼져 가는 촛불", "푸른 혼불", "영원히 타는 검은 촛불"},
                {"늙은 까마귀", "은빛 눈의 까마귀", "저승길 까마귀 왕"},
                {"이 빠진 낫", "흑철 낫", "저승사자의 낫"},
                {"금 간 모래시계", "피가 흐르는 모래시계", "수명을 세는 모래시계"},
                {"썩은 나무 관", "흑단 관", "왕들이 잠든 석관"},
                {"뼈로 깎은 반지", "검은 진주 반지", "저승 왕의 인장 반지"},
                {"시든 국화", "핏빛 꽃송이", "황천의 꽃"},
                {"금 간 놋종", "은빛 장례 종", "망자를 부르는 종"},
                {"흐릿한 그림자", "형체 있는 그림자", "죽음 그 자체의 그림자"},
                {"녹슨 동전 한 닢", "은빛 뱃삯 동전", "뱃사공의 황금 뱃삯"},
        });
        FORMS.put(AiType.STRUGGLE, new String[][]{
                {"이 빠진 검", "은빛 장검", "전쟁 영웅의 성검"},
                {"금 간 나무 방패", "강철 탑 방패", "거인의 방패"},
                {"찢어진 군기", "붉은 전쟁 깃발", "천 번 이긴 군대의 깃발"},
                {"찢어진 북", "청동 전쟁 나팔", "천둥을 부르는 뿔나팔"},
                {"찌그러진 투구", "뿔 달린 강철 투구", "전쟁신의 황금 투구"},
                {"부러진 화살", "불붙은 화살", "하늘을 꿰뚫는 화살"},
                {"해진 가죽 장갑", "강철 손갑옷", "용을 꺾은 손갑옷"},
                {"녹슨 쇠사슬", "검은 강철 사슬", "신을 묶었던 사슬"},
                {"피 묻은 붕대", "끓어오르는 피", "전쟁신의 피 한 방울"},
                {"굶주린 들개", "은빛 늑대", "달을 삼킨 늑대"},
                {"녹슨 창", "번개 깃든 창", "군신의 장창"},
        });
        FORMS.put(AiType.TREASURE, new String[][]{
                {"녹슨 구리 동전", "반짝이는 금화", "용이 품던 금화"},
                {"낡은 나무 상자", "은으로 테두른 상자", "왕의 보물 상자"},
                {"흐린 유리 구슬", "붉은 루비", "용의 눈 보석"},
                {"녹슨 철 머리띠", "은빛 관", "고대 황제의 왕관"},
                {"탁한 진주", "흑진주", "인어 여왕의 진주"},
                {"이 빠진 잔", "은 술잔", "신들의 황금 성배"},
                {"해진 동전 주머니", "비단 금 주머니", "끝없이 나오는 주머니"},
                {"부러진 지팡이", "보석 박힌 홀", "지혜왕의 홀"},
                {"조개껍데기 목걸이", "금 목걸이", "여신의 별 목걸이"},
                {"바랜 물고기 비늘", "황금 잉어 비늘", "황금룡의 비늘"},
                {"벌레 먹은 사과", "황금 사과", "천도복숭아"},
        });
    }

    // 꿈속 천상에서 신이 머무는 곳. 빛 묘사("눈부신", "찬란한")를 쓰다 외국 글자가 자주 새어, 장소와 물건을 그리게 한다
    private static final Map<AiType, String[]> PLACES = new EnumMap<>(AiType.class);

    static {
        PLACES.put(AiType.ADVENTURE, new String[]{"구름 위 낡은 선착장", "바람 부는 절벽 길", "끝없이 이어진 돌다리", "별 지도가 새겨진 탑 꼭대기"});
        PLACES.put(AiType.CHAOS, new String[]{"거꾸로 흐르는 폭포 아래", "뒤틀린 계단 정원", "웃음소리가 울리는 유리 미로", "색이 바뀌는 연못가"});
        PLACES.put(AiType.DEATH, new String[]{"잿빛 강가", "촛불이 늘어선 납골당", "시든 꽃밭", "안개 낀 나루터"});
        PLACES.put(AiType.STRUGGLE, new String[]{"피 묻은 원형 투기장", "부서진 성벽 위", "창이 꽂힌 전장 언덕", "북소리가 울리는 연무장"});
        PLACES.put(AiType.TREASURE, new String[]{"금화가 쌓인 보물고", "보석 박힌 회랑", "자물쇠로 가득한 창고", "황금 저울이 놓인 제단"});
    }

    private GodForms() {}

    static String place(AiType god, Random rnd) {
        String[] places = PLACES.get(god);
        return places[rnd.nextInt(places.length)];
    }

    static List<String> places(AiType god) {
        return java.util.Arrays.asList(PLACES.get(god));
    }

    /** 등급을 확률대로 뽑고, 그 신의 11가지 형상 중 하나를 고른다 */
    static Form pick(AiType god, Random rnd) {
        int roll = rnd.nextInt(100);
        Grade grade = Grade.HIGH;
        for (Grade g : Grade.values()) {
            if (roll < g.weight) {
                grade = g;
                break;
            }
            roll -= g.weight;
        }
        String[][] families = FORMS.get(god);
        return new Form(families[rnd.nextInt(families.length)][grade.ordinal()], grade);
    }

    static List<Form> all(AiType god) {
        List<Form> list = new ArrayList<>();
        for (String[] family : FORMS.get(god)) {
            for (Grade g : Grade.values()) list.add(new Form(family[g.ordinal()], g));
        }
        return list;
    }
}
