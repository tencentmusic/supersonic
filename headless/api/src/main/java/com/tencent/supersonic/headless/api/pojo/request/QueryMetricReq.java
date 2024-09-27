package com.tencent.supersonic.headless.api.pojo.request;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.Filter;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class QueryMetricReq {

    private Long domainId;

    private List<Long> metricIds = Lists.newArrayList();

    private List<String> metricNames = Lists.newArrayList();

    private List<Long> dimensionIds = Lists.newArrayList();

    private List<String> dimensionNames = Lists.newArrayList();

    private List<Filter> filters = Lists.newArrayList();

    private DateConf dateInfo = new DateConf();

    private long limit = Constants.DEFAULT_METRIC_LIMIT;

    private boolean innerLayerNative = false;
}
