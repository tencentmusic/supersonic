package com.tencent.supersonic.headless.server.pojo;

import java.util.List;
import lombok.Data;

@Data
public class MetricsFilter extends MetaFilter {

    private List<Long> modelIds;

    private List<Long> metricIds;

    private List<String> metricNames;

}
