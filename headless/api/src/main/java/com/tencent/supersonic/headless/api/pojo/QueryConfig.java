package com.tencent.supersonic.headless.api.pojo;

import lombok.Data;

import java.io.Serializable;

@Data
public class QueryConfig implements Serializable {

    private DetailTypeDefaultConfig detailTypeDefaultConfig = new DetailTypeDefaultConfig();

    private AggregateTypeDefaultConfig aggregateTypeDefaultConfig =
            new AggregateTypeDefaultConfig();
}
