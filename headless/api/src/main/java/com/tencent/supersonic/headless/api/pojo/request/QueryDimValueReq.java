package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.common.pojo.DateConf;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class QueryDimValueReq {

    private Long modelId;
    private String dimensionBizName;
    private String value;
    private DateConf dateInfo;

}
