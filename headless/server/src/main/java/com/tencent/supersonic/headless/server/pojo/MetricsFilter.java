package com.tencent.supersonic.headless.server.pojo;

import java.util.List;

import com.tencent.supersonic.headless.api.pojo.MetaFilter;
import lombok.Data;

@Data
public class MetricsFilter extends MetaFilter {

    private List<Long> modelIds;

    private List<Long> metricIds;

    private List<String> metricNames;

}
