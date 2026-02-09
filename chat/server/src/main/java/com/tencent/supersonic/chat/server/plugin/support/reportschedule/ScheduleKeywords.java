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
    public static final List<String> CONFIRM = Arrays.asList("确认", "是", "好的", "确定", "yes", "ok");
    public static final List<String> CANCEL = Arrays.asList("取消", "删除", "停止");
    public static final List<String> PAUSE = Arrays.asList("暂停");
    public static final List<String> RESUME = Arrays.asList("恢复", "继续");
    public static final List<String> TRIGGER = Arrays.asList("现在", "立即", "马上");
    public static final List<String> STATUS = Arrays.asList("执行情况", "执行记录", "执行状态");
    public static final List<String> LIST_PREFIX = Arrays.asList("有哪些", "列表", "查看", "我的");
    public static final List<String> LIST_SUFFIX = Arrays.asList("定时", "报表", "调度");
    public static final List<String> CREATE = Arrays.asList("每天", "每周", "每月", "定时", "发送", "推送");

    // Time period keywords for parsing
    public static final List<String> AFTERNOON = Arrays.asList("下午", "晚上");

    // Time pattern: (早上|上午|下午|晚上)?(\d{1,2})[点时]
    public static final Pattern TIME_PATTERN = Pattern.compile("(早上|上午|下午|晚上)?(\\d{1,2})[点时]");

    // Schedule ID pattern: #?(\d+)
    public static final Pattern SCHEDULE_ID_PATTERN = Pattern.compile("#?(\\d+)");

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
        // Monthly patterns
        CRON_PATTERNS.put("每月1号", "0 0 9 1 * ?");
        CRON_PATTERNS.put("每月15号", "0 0 9 15 * ?");
        // General patterns (less specific, check last)
        CRON_PATTERNS.put("每小时", "0 0 * * * ?");
        CRON_PATTERNS.put("每天", "0 0 9 * * ?");
    }

    // Day of week translations
    public static final Map<String, String> DAY_OF_WEEK = Map.of("MON", "一", "TUE", "二", "WED", "三",
            "THU", "四", "FRI", "五", "SAT", "六", "SUN", "日");
}
