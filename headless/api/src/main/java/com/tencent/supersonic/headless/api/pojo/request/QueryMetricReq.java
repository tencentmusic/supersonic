package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.Filter;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@ToString
public class QueryMetricReq {

    private Long domainId;

    private List<Long> metricIds;

    private List<String> metricNames;

    private List<Long> dimensionIds;

    private List<String> dimensionNames;

    private List<Filter> filters = new ArrayList<>();

    private DateConf dateInfo = new DateConf();

    private Long limit = 2000L;

}