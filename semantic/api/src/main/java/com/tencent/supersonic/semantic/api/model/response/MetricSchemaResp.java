package com.tencent.supersonic.semantic.api.model.response;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(callSuper = true)
public class MetricSchemaResp extends MetricResp {

    private Long useCnt = 0L;

}