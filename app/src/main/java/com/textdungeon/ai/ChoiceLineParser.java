package com.textdungeon.ai;

import com.textdungeon.model.RewardStat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "키: 값" 줄 형식의 AI 응답을 ChoiceDelta로 바꾸는 관대한 파서.
 *
 * 기대 형식:
 *   선택지: 제단에 손을 얹는다
 *   보상설명: 뜨거운 기운이 팔을 타고 오른다. 힘 12, 경험치 30 증가
 *   아이템: 없음
 *   스탯: 힘=12
 *   스탯: 경험치=30
 *
 * 모델은 "체력"을 HP로 이해하는 경우가 많아서, HP 변화는 "HP-12", "HP+10"으로 받고
 * 게임 키워드로 바꾼다 (HP-12 → 데미지 12, HP+10 → 회복 10).
 *
 * JSON과 달리 한 줄이 깨져도 그 줄만 버리고 나머지는 살린다.
 * 소형 온디바이스 모델이 형식을 조금 어겨도 버틸 수 있게 하는 게 목적.
 */
public final class ChoiceLineParser {

    /** Reward.apply()와 Stat.gainStat()이 실제로 처리하는 키워드만 허용 */
    public static final Set<String> ALLOWED_STATS = new HashSet<>(Arrays.asList(
            "힘", "민첩", "체력", "지혜", "경험치", "데미지", "회복", "골드"));

    private static final int MAX_CHOICE_LENGTH = 40;
    private static final int MAX_ABS_VALUE = 9999;
    /** 보상설명이 묘사 문장인지 보는 최소 한글 글자 수. "120", "10 회복"처럼 숫자 위주인 설명을 거른다 */
    private static final int MIN_DESCRIPTION_HANGUL = 6;

    // 프롬프트 안내문을 베낀 선택지: "(20자 내외의 행동)", "(2) 기존 선택지" 처럼 괄호로 시작하거나 글자 수 안내가 들어 있음
    private static final Pattern PLACEHOLDER_CHOICE =
            Pattern.compile("^[(\\[<（【].*|.*\\d+\\s*자\\s*(내외|이내|안팎).*");

    // "힘=12", "힘 12", "힘:12", "힘 +12" 모두 허용
    private static final Pattern STAT_PATTERN =
            Pattern.compile("(힘|민첩|체력|지혜|경험치|데미지|회복|골드|HP|hp|Hp)\\s*[=:]?\\s*([+-]?\\d+)");

    private ChoiceLineParser() {}

    public static class ParseException extends Exception {
        public ParseException(String message) { super(message); }
    }

    public static ChoiceDelta parse(String raw, Set<String> validItemIds) throws ParseException {
        return parse(raw, validItemIds, Collections.emptySet());
    }

    /**
     * @param raw              AI 응답 원문
     * @param validItemIds     실제 존재하는 아이템 id 목록 (여기 없는 id는 버린다)
     * @param forbiddenChoices 그대로 나오면 실패로 볼 선택지 (기존 선택지, 프롬프트 예시). 띄어쓰기 차이는 무시한다
     */
    public static ChoiceDelta parse(String raw, Set<String> validItemIds, Set<String> forbiddenChoices)
            throws ParseException {
        if (raw == null || raw.trim().isEmpty()) {
            throw new ParseException("빈 응답");
        }

        String choice = null;
        String description = null;
        String itemId = null;
        List<RewardStat> stats = new ArrayList<>();

        for (String rawLine : raw.split("\\r?\\n")) {
            String line = cleanLine(rawLine);
            if (line.isEmpty() || line.startsWith("```")) continue;

            int colon = line.indexOf(':');
            if (colon <= 0) continue; // 키가 없는 줄은 무시
            String key = line.substring(0, colon).replace(" ", "");
            String value = line.substring(colon + 1).trim(); // 첫 콜론 기준으로만 자름 → 설명 속 ':' 보존

            switch (key) {
                case "선택지":
                case "선택":
                    if (choice == null) choice = cleanText(value);
                    break;
                case "보상설명":
                case "결과":
                case "설명":
                    if (description == null) description = cleanText(value);
                    break;
                case "아이템":
                case "아이템id":
                    itemId = parseItem(value, validItemIds);
                    break;
                case "스탯":
                case "보상":
                    parseStats(value, stats);
                    break;
                default:
                    // 모르는 키는 무시
                    break;
            }
        }

        if (choice == null || choice.isEmpty()) {
            throw new ParseException("선택지 누락");
        }
        if (description == null || description.isEmpty()) {
            throw new ParseException("보상설명 누락");
        }
        // 형식은 맞아도 게임에 붙이면 안 되는 내용 (소형 모델이 안내문이나 기존 선택지를 베끼는 경우)
        if (PLACEHOLDER_CHOICE.matcher(choice).matches()) {
            throw new ParseException("선택지가 안내문 그대로");
        }
        for (String forbidden : forbiddenChoices) {
            if (forbidden != null && withoutSpaces(forbidden).equals(withoutSpaces(choice))) {
                throw new ParseException("선택지가 기존 선택지나 예시와 같음");
            }
        }
        if (countHangul(description) < MIN_DESCRIPTION_HANGUL) {
            throw new ParseException("보상설명이 묘사 문장이 아님");
        }
        if (choice.length() > MAX_CHOICE_LENGTH) {
            choice = choice.substring(0, MAX_CHOICE_LENGTH);
        }
        return new ChoiceDelta(choice, description, itemId, stats);
    }

    /** 목록 기호, 마크다운 굵게, 전각 콜론을 정리 */
    private static String cleanLine(String line) {
        String s = line.trim()
                .replace('：', ':')
                .replace("**", "");
        s = s.replaceFirst("^([-*•]|\\d+[.)])\\s*", "");
        return s.trim();
    }

    /** 앞뒤 따옴표 제거 */
    private static String cleanText(String s) {
        String t = s.trim();
        while (t.length() >= 2 && isQuote(t.charAt(0)) && isQuote(t.charAt(t.length() - 1))) {
            t = t.substring(1, t.length() - 1).trim();
        }
        return t;
    }

    private static String withoutSpaces(String s) {
        return s.replaceAll("\\s+", "");
    }

    private static int countHangul(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '가' && c <= '힣') n++;
        }
        return n;
    }

    private static boolean isQuote(char c) {
        return c == '"' || c == '\'' || c == '“' || c == '”' || c == '「' || c == '」';
    }

    private static String parseItem(String value, Set<String> validItemIds) {
        String v = cleanText(value);
        if (v.isEmpty()) return null;
        String lower = v.toLowerCase();
        if (lower.equals("없음") || lower.equals("null") || lower.equals("none") || lower.equals("-")) {
            return null;
        }
        // "item_01:낡은 검"처럼 이름까지 붙여서 오는 경우 id만 사용
        int colon = v.indexOf(':');
        if (colon > 0) v = v.substring(0, colon).trim();
        return validItemIds != null && validItemIds.contains(v) ? v : null;
    }

    /** "힘=12, 경험치=30" 처럼 한 줄에 여러 개가 와도 처리 */
    private static void parseStats(String value, List<RewardStat> out) {
        Matcher m = STAT_PATTERN.matcher(value);
        while (m.find()) {
            String type = m.group(1);
            int amount;
            try {
                amount = Integer.parseInt(m.group(2));
            } catch (NumberFormatException e) {
                continue;
            }
            if (amount == 0) continue;
            // HP 변화는 게임 키워드로: 줄면 데미지, 늘면 회복
            if (type.equalsIgnoreCase("HP")) {
                type = amount < 0 ? "데미지" : "회복";
            }
            // 데미지/회복은 항상 양수로 처리 (Reward.apply 규칙)
            if (type.equals("데미지") || type.equals("회복")) {
                amount = Math.abs(amount);
            }
            amount = Math.max(-MAX_ABS_VALUE, Math.min(MAX_ABS_VALUE, amount));
            out.add(new RewardStat(type, amount));
        }
    }
}
