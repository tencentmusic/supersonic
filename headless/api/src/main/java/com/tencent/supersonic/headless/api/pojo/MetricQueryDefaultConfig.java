package com.tencent.supersonic.headless.api.pojo;

import com.tencent.supersonic.common.pojo.RecordInfo;
import lombok.Data;

@Data
public class MetricQueryDefaultConfig extends RecordInfo {

    private Long id;

    private Long metricId;

    private String userName;

    // string of queryStruct
    private String defaultConfig;
}
