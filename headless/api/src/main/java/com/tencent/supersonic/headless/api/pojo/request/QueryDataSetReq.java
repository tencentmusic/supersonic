package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.Filter;
import com.tencent.supersonic.common.pojo.Order;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.headless.api.pojo.Cache;
import com.tencent.supersonic.headless.api.pojo.Param;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@ToString
public class QueryDataSetReq {

    private Long dataSetId;
    private String dataSetName;
    private String sql;
    private boolean needAuth = true;
    private List<Param> params = new ArrayList<>();
    private Cache cacheInfo = new Cache();
    private List<String> groups = new ArrayList<>();
    private List<Aggregator> aggregators = new ArrayList<>();
    private List<Order> orders = new ArrayList<>();
    private List<Filter> dimensionFilters = new ArrayList<>();
    private List<Filter> metricFilters = new ArrayList<>();
    private DateConf dateInfo;
    private Long limit = 2000L;
    private QueryType queryType = QueryType.DETAIL;
    private boolean innerLayerNative = false;
}
