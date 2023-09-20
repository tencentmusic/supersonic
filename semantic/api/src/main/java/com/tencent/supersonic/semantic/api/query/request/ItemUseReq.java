package com.tencent.supersonic.semantic.api.query.request;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
public class ItemUseReq {

    private String startTime;
    private Long modelId;
    private List<Long> modelIds;
    private Boolean cacheEnable = true;
    private String metric;

    public ItemUseReq(String startTime, Long modelId) {
        this.startTime = startTime;
        this.modelId = modelId;
    }
    public ItemUseReq(String startTime, List<Long> modelIds) {
        this.startTime = startTime;
        this.modelIds = modelIds;
    }
}
