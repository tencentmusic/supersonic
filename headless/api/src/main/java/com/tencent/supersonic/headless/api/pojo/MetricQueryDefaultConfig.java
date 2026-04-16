package com.tencent.supersonic.headless.api.pojo;

import com.tencent.supersonic.common.pojo.RecordInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class MetricQueryDefaultConfig extends RecordInfo {

    private Long id;

    private Long metricId;

    private String userName;

    // string of queryStruct
    private String defaultConfig;
}
