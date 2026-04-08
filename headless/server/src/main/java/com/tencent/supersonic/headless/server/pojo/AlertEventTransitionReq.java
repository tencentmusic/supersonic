package com.tencent.supersonic.headless.server.pojo;

import lombok.Data;

@Data
public class AlertEventTransitionReq {
    private AlertResolutionStatus targetStatus;
    private Long assigneeId;
    private String notes;
}
