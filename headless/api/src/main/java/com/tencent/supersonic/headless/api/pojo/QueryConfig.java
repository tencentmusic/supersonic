package com.tencent.supersonic.headless.api.pojo;

import lombok.Data;

import java.io.Serializable;

@Data
public class QueryConfig implements Serializable {

    private DetailTypeDefaultConfig detailTypeDefaultConfig = new DetailTypeDefaultConfig();

    private AggregateTypeDefaultConfig aggregateTypeDefaultConfig =
            new AggregateTypeDefaultConfig();

    /**
     * SQL template config for complex reports (UNION, window functions, etc.). When set, takes
     * priority over structured query configs above. Uses ST4 syntax for parameter rendering.
     */
    private SqlTemplateConfig sqlTemplateConfig;
}
