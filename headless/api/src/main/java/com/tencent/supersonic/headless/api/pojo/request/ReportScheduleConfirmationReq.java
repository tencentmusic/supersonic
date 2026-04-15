package com.tencent.supersonic.headless.api.pojo.request;

import lombok.Data;

import java.util.Date;

@Data
public class ReportScheduleConfirmationReq {
    private String confirmToken;
    private Long userId;
    private Integer chatId;
    private String actionType;
    private Long sourceQueryId;
    private Integer sourceParseId;
    private Long sourceDataSetId;
    private String payloadJson;
    private Date expireAt;
    private Date createdAt;
    private Long tenantId;
}
