package com.tencent.supersonic.headless.api.pojo;

import lombok.Data;

@Data
public abstract class MetricDefineParams {

    private String expr;

    private String filterSql;
}
