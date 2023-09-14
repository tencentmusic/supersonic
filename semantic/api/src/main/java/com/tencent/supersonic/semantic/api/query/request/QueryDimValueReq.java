package com.tencent.supersonic.semantic.api.query.request;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class QueryDimValueReq {

    private Long modelId;
    private String dimensionBizName;
    private String value;

}
