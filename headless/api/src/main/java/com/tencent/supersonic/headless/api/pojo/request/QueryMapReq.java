package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.QueryDataType;
import com.tencent.supersonic.headless.api.pojo.enums.MapModeEnum;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class QueryMapReq {
    private String queryText;
    private List<String> dataSetNames;
    private User user;
    private Integer topN = 0;
    private MapModeEnum mapModeEnum = MapModeEnum.STRICT;
    private QueryDataType queryDataType = QueryDataType.ALL;
}
