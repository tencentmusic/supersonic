package com.tencent.supersonic.headless.server.pojo;

import com.tencent.supersonic.headless.api.pojo.MetaFilter;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class MetricsFilter extends MetaFilter {

    private List<Long> modelIds;

    private List<Long> metricIds;

    private List<String> metricNames;
}
