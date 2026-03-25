package com.tencent.supersonic.chat.server.plugin.support.reportschedule;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Keywords and patterns for schedule intent recognition.
 */
public final class ScheduleKeywords {

    private ScheduleKeywords() {}

    // Intent recognition keywords
    public static final List<String> CONFIRM =
            Arrays.asList("确认", "是", "好的", "确定", "yes", "ok", "发送", "推送");
    public static final List<String> CANCEL = Arrays.asList("取消", "删除", "停止");
    public static final List<String> PAUSE = Arrays.asList("暂停");
    public static final List<String> RESUME = Arrays.asList("恢复", "继续");
    public static final List<String> TRIGGER = Arrays.asList("现在", "立即", "马上");
    public static final List<String> STATUS = Arrays.asList("执行情况", "执行记录", "执行状态");
    public static final List<String> LIST_PREFIX = Arrays.asList("有哪些", "列表", "查看", "我的");
    public static final List<String> LIST_SUFFIX = Arrays.asList("定时", "报表", "调度");
    public static final List<String> CREATE = Arrays.asList("每天", "每周", "每月", "定时", "发送", "推送");
    public static final List<String> FREQUENCY = Arrays.asList("每小时", "每天", "每周", "每月", "定时");
    public static final List<String> CREATE_VERBS = Arrays.asList("创建", "设成", "保存成", "保存", "订阅",
            "推送", "发送", "发给我", "发我", "推给我", "推送给我", "给我发送", "给我推送");
    public static final List<String> CONTEXT_REFERENCES = Arrays.asList("基于刚才", "基于我刚才查询的",
            "基于刚才那个报表", "刚才那个报表", "这个报表", "这个结果", "沿用刚才条件", "上一个报表", "刚才这个报表");
    public static final List<String> LIST_PHRASES =
            Arrays.asList("我的定时报表", "我的报表任务", "有哪些定时报表", "查看报表任务", "报表列表", "我的调度任务");

    /**
     * Action verbs that indicate scheduling intent, but only when combined with a frequency word
     * from CREATE. Alone they are too ambiguous ("帮我查数据发给我" should not trigger scheduling).
     */
    public static final List<String> CREATE_ACTION =
            Arrays.asList("发给我", "发我", "发给", "发一下", "推给", "推一下", "订阅", "设成", "保存成");

    /**
     * Triggers an immediate execution after schedule creation. Distinct from TRIGGER (which
     * requires a #ID and operates on existing schedules).
     */
    public static final List<String> TRIGGER_NOW =
            Arrays.asList("现在先", "先发一次", "先推送一次", "现在就", "立即执行一次", "立即", "马上");

    // Time period keywords for parsing
    public static final List<String> AFTERNOON = Arrays.asList("下午", "晚上");

    // Time pattern: (早上|上午|下午|晚上)? followed by H:MM, H点半, or H点/H时
    // Group 1: period prefix (optional), Group 2: hour,
    // Group 4: minute digits (HH:MM format only), Group 5: "半" (30 min)
    public static final Pattern TIME_PATTERN =
            Pattern.compile("(早上|上午|下午|晚上)?\\s*(\\d{1,2})(?:(:|：)(\\d{2})|[点时](半)?)");

    // Schedule ID pattern: #(\d+) — # prefix is required to avoid false positives like "取消3个报表"
    public static final Pattern SCHEDULE_ID_PATTERN = Pattern.compile("#(\\d+)");

    // Cron expression patterns (ordered by specificity)
    public static final Map<String, String> CRON_PATTERNS = new LinkedHashMap<>();

    static {
        // Weekly patterns (more specific, check first)
        CRON_PATTERNS.put("每周一", "0 0 9 ? * MON");
        CRON_PATTERNS.put("每周二", "0 0 9 ? * TUE");
        CRON_PATTERNS.put("每周三", "0 0 9 ? * WED");
        CRON_PATTERNS.put("每周四", "0 0 9 ? * THU");
        CRON_PATTERNS.put("每周五", "0 0 9 ? * FRI");
        CRON_PATTERNS.put("每周六", "0 0 9 ? * SAT");
        CRON_PATTERNS.put("每周日", "0 0 9 ? * SUN");
        CRON_PATTERNS.put("每周", "0 0 9 ? * MON"); // fallback: default to Monday
        // Monthly patterns
        CRON_PATTERNS.put("每月1号", "0 0 9 1 * ?");
        CRON_PATTERNS.put("每月15号", "0 0 9 15 * ?");
        CRON_PATTERNS.put("每月", "0 0 9 1 * ?"); // fallback: default to 1st
        // General patterns (less specific, check last)
        CRON_PATTERNS.put("每小时", "0 0 * * * ?");
        CRON_PATTERNS.put("每天", "0 0 9 * * ?");
    }

    // Day of week translations
    public static final Map<String, String> DAY_OF_WEEK = Map.of("MON", "一", "TUE", "二", "WED", "三",
            "THU", "四", "FRI", "五", "SAT", "六", "SUN", "日");

    public static boolean containsAny(String text, List<String> phrases) {
        return text != null && phrases.stream().anyMatch(text::contains);
    }

    public static boolean hasTimeExpression(String text) {
        return text != null && TIME_PATTERN.matcher(text).find();
    }

    public static boolean hasFrequencyExpression(String text) {
        return containsAny(text, FREQUENCY);
    }

    public static boolean hasCreateVerb(String text) {
        return containsAny(text, CREATE_VERBS) || containsAny(text, CREATE_ACTION);
    }

    public static boolean hasContextReference(String text) {
        return containsAny(text, CONTEXT_REFERENCES);
    }

    public static boolean isHardCreate(String text) {
        boolean hasCreateVerb = hasCreateVerb(text);
        boolean hasContextReference = hasContextReference(text);
        boolean hasFrequencyExpression = hasFrequencyExpression(text);
        boolean hasTimeExpression = hasTimeExpression(text);
        return (hasCreateVerb && hasContextReference)
                || (hasCreateVerb && hasFrequencyExpression && hasTimeExpression)
                || (hasContextReference && hasFrequencyExpression && hasTimeExpression);
    }

    public static boolean isHardList(String text) {
        return containsAny(text, LIST_PHRASES) && !hasCreateVerb(text) && !hasTimeExpression(text)
                && !hasContextReference(text);
    }

    public static int createScore(String text) {
        int score = 0;
        if (hasCreateVerb(text)) {
            score += 2;
        }
        if (hasFrequencyExpression(text)) {
            score += 2;
        }
        if (hasTimeExpression(text)) {
            score += 1;
        }
        if (hasContextReference(text)) {
            score += 2;
        }
        if (containsAny(text, TRIGGER_NOW)) {
            score += 1;
        }
        return score;
    }

    public static int listScore(String text) {
        int score = 0;
        if (containsAny(text, LIST_PHRASES)) {
            score += 3;
        }
        if (containsAny(text, LIST_PREFIX) && containsAny(text, LIST_SUFFIX)) {
            score += 1;
        }
        if (hasCreateVerb(text) || hasTimeExpression(text) || hasContextReference(text)) {
            score -= 4;
        }
        return score;
    }

    public static boolean preferCreate(String text) {
        if (isHardCreate(text)) {
            return true;
        }
        if (isHardList(text)) {
            return false;
        }
        return createScore(text) >= 3 && createScore(text) > listScore(text);
    }

    public static boolean preferList(String text) {
        if (isHardList(text)) {
            return true;
        }
        if (isHardCreate(text)) {
            return false;
        }
        return listScore(text) >= 3 && listScore(text) > createScore(text);
    }
}
