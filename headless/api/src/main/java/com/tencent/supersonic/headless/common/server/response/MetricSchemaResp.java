package com.tencent.supersonic.headless.common.server.response;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(callSuper = true)
public class MetricSchemaResp extends MetricResp {

    private Long useCnt = 0L;

}