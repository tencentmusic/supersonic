package com.tencent.supersonic.headless.api.pojo;

import lombok.Data;

@Data
public class QueryConfig {

    private DetailTypeDefaultConfig detailTypeDefaultConfig = new DetailTypeDefaultConfig();

    private AggregateTypeDefaultConfig aggregateTypeDefaultConfig =
            new AggregateTypeDefaultConfig();
}
