package com.tencent.supersonic.chat.server.plugin.support.reportschedule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSubscriptionSource {

    private Long sourceQueryId;
    private Integer sourceParseId;
    private Long sourceDataSetId;
    private String sourceType;
    private String queryConfigSnapshot;
    private String summaryText;
}
