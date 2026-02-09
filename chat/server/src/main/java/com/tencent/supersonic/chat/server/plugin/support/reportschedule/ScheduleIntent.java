package com.tencent.supersonic.chat.server.plugin.support.reportschedule;

/**
 * Intent types for report schedule plugin.
 */
public enum ScheduleIntent {
    /** Create a new schedule: "每天9点发送报表" */
    CREATE,
    /** List user's schedules: "我的定时报表有哪些" */
    LIST,
    /** Cancel a schedule: "取消报表123" */
    CANCEL,
    /** Pause a schedule: "暂停报表123" */
    PAUSE,
    /** Resume a schedule: "恢复报表123" */
    RESUME,
    /** Trigger immediately: "现在就发一份" */
    TRIGGER,
    /** Check execution status: "报表123执行情况" */
    STATUS,
    /** Confirm a pending action: "确认" */
    CONFIRM,
    /** Unknown intent */
    UNKNOWN
}
