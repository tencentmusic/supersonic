package com.tencent.supersonic.headless.api.pojo.response;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(callSuper = true)
public class MetricSchemaResp extends MetricResp {

    private Long useCnt = 0L;

}