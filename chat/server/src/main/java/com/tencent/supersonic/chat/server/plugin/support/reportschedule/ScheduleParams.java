package com.tencent.supersonic.chat.server.plugin.support.reportschedule;

import lombok.Data;

@Data
public class ScheduleParams {
    private String name;
    private String cronExpression;
    private String outputFormat = "EXCEL";
}
