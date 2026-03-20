package com.tencent.supersonic.chat.server.plugin.support.reportschedule;

/**
 * Message templates for schedule plugin responses. Supports future i18n by centralizing all
 * user-facing messages.
 */
public final class ScheduleMessages {

    private ScheduleMessages() {}

    // Error messages
    public static final String ERROR_OPERATION_FAILED = "操作失败: %s";
    public static final String ERROR_SESSION_MISSING = "无法确认：会话信息缺失";
    public static final String ERROR_NO_PENDING = "没有待确认的操作，或确认已超时（5分钟）";
    public static final String ERROR_UNSUPPORTED_CONFIRM = "不支持的确认操作";
    public static final String ERROR_SCHEDULE_NOT_FOUND = "未找到编号为 #%d 的报表任务";
    public static final String ERROR_SPECIFY_FREQUENCY = "请告诉我发送频率，比如「每天早上9点」或「每周一」";
    public static final String ERROR_SPECIFY_REPORT_CONTENT =
            "还缺少要订阅的报表内容。请先在当前会话里查出报表结果，再说「每天9点发送这个报表」。";
    public static final String ERROR_UNSUPPORTED_REPORT_CONTEXT =
            "当前会话查询含有具体日期条件，不支持直接定时订阅。请使用结构化查询（指定指标和维度而非自定义 SQL）后再创建定时报表。";
    public static final String ERROR_NO_PERMISSION = "无权操作此任务，只有创建者可以管理";
    public static final String ERROR_NO_DELIVERY_CONFIG =
            "当前租户还没有启用可用的投递通道。请先配置飞书、邮件或 Webhook 推送，再创建定时报表。";
    public static final String ERROR_SPECIFY_CANCEL_ID = "请指定要取消的报表编号，例如「取消报表 #123」";
    public static final String ERROR_SPECIFY_PAUSE_ID = "请指定要暂停的报表编号，例如「暂停报表 #123」";
    public static final String ERROR_SPECIFY_RESUME_ID = "请指定要恢复的报表编号，例如「恢复报表 #123」";
    public static final String ERROR_SPECIFY_TRIGGER_ID = "请指定要执行的报表编号，例如「立即执行报表 #123」";
    public static final String ERROR_SPECIFY_STATUS_ID = "请指定要查看的报表编号，例如「报表 #123 的执行情况」";

    // Success messages args: scheduleId, cronDesc, channelName, scheduleId
    public static final String SUCCESS_CREATED = "已创建定时报表（#%d）\n频率：%s\n推送至：%s\n\n您可以说「取消报表 #%d」来停止";
    public static final String SUCCESS_CREATED_WITH_TRIGGER =
            "✅ 定时报表（#%d）已创建，频率：%s\n推送至：%s\n🚀 已触发一次立即执行，请留意推送渠道。\n\n您可以说「取消报表 #%d」来停止";
    public static final String SUCCESS_CANCELLED = "已取消定时报表 #%d";
    public static final String SUCCESS_PAUSED = "已暂停定时报表 #%d\n\n您可以说「恢复报表 #%d」来重新启用";
    public static final String SUCCESS_RESUMED = "已恢复定时报表 #%d";
    public static final String SUCCESS_TRIGGERED = "已触发报表 #%d 的执行，请稍后查看结果";

    // Confirmation messages args: sourceSummary, cronDescription
    public static final String CONFIRM_CREATE = "我将为您创建定时报表：\n报表：%s\n频率：%s\n\n确认创建吗？（回复「确认」）";
    public static final String CONFIRM_CREATE_WITH_TRIGGER =
            "我将为您创建定时报表：\n报表：%s\n频率：%s\n创建后立即执行一次 ✓\n\n确认创建吗？（回复「确认」）";
    public static final String CONFIRM_CANCEL = "确认要取消「%s」(#%d) 的定时推送吗？（回复「确认」）";

    // List messages
    public static final String LIST_EMPTY = "您当前没有定时报表任务。\n\n您可以说「基于刚才那个报表，每天9:30发给我」来创建一个。";
    public static final String LIST_HEADER = "您当前有 %d 个定时报表：\n";
    public static final String LIST_ITEM = "\n#%d %s\n├ 频率：%s\n└ 状态：%s";
    public static final String STATUS_RUNNING = "运行中";
    public static final String STATUS_PAUSED = "已暂停";

    // Status/execution messages
    public static final String EXECUTION_EMPTY = "报表 #%d 暂无执行记录";
    public static final String EXECUTION_HEADER = "报表 #%d 最近执行记录：\n";
    public static final String EXECUTION_ERROR = "\n   错误：%s";

    // Unknown intent help
    public static final String UNKNOWN_INTENT = """
            我不太理解您的意图。您可以说：
            • 每天9:30发给我（基于当前报表结果）
            • 基于刚才那个报表，每天9:30推送给我
            • 把刚才这个报表保存成定时任务，现在先发一次
            • 我的定时报表有哪些
            • 取消报表 #123
            • 暂停/恢复报表 #123
            • 立即执行报表 #123""";

    // Cron description
    public static final String CRON_UNKNOWN = "未知";
    public static final String CRON_HOURLY = "每小时";
    public static final String CRON_DAILY = "每天 ";
    public static final String CRON_WEEKLY = "每周%s ";
    public static final String CRON_MONTHLY = "每月%s号 ";

    // Schedule name template
    public static final String SCHEDULE_NAME_TEMPLATE = "定时报表 - %s";
}
