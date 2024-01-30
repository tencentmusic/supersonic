package com.tencent.supersonic.headless.api.pojo;

import lombok.Data;

@Data
public class QueryConfig {

    private TagTypeDefaultConfig tagTypeDefaultConfig = new TagTypeDefaultConfig();

    private MetricTypeDefaultConfig metricTypeDefaultConfig = new MetricTypeDefaultConfig();


}
