package com.tencent.supersonic.headless.query.parser.calcite.s2sql;

import lombok.Data;


@Data
public class DimensionTimeTypeParams {

    private String isPrimary;

    private String timeGranularity;
}
