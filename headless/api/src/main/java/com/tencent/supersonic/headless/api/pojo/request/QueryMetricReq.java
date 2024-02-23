package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.common.pojo.DateConf;
import java.util.List;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class QueryMetricReq {

    private Long domainId;

    private List<Long> metricIds;

    private List<String> metricNames;

    private List<Long> dimensionIds;

    private List<String> dimensionNames;

    private DateConf dateInfo = new DateConf();

    private Long limit = 2000L;

}