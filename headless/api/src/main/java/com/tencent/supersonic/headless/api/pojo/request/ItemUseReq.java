package com.tencent.supersonic.headless.api.pojo.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@ToString
@NoArgsConstructor
public class ItemUseReq {

    private String startTime;
    private Long modelId;
    private Long dataSetId;
    private List<Long> dataSetIds;
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
