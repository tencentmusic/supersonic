package com.tencent.supersonic.headless.api.pojo.response;

import lombok.Data;

import java.util.Date;

@Data
public class ReportScheduleConfirmationResp {
    private Long id;
    private String confirmToken;
    private Long userId;
    private Integer chatId;
    private String actionType;
    private Long sourceQueryId;
    private Integer sourceParseId;
    private Long sourceDataSetId;
    private String payloadJson;
    private String status;
    private Date expireAt;
    private Date createdAt;
    private Long tenantId;
}
