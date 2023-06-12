package com.tencent.supersonic.semantic.api.query.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
public class ItemUseReq {

    private String startTime;
    private Long domainId;
    private Boolean cacheEnable = true;
    private String metric;

    public ItemUseReq(String startTime, Long domainId) {
        this.startTime = startTime;
        this.domainId = domainId;
    }
}
