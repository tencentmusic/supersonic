package com.tencent.supersonic.headless.api.pojo.request;

import lombok.Data;

@Data
public class ReportDeliveryConfigReq {
    private Long id;
    private String name;
    private String deliveryType;
    private String deliveryConfig;
    private Boolean enabled;
    private String description;
    private Integer maxConsecutiveFailures;
}
