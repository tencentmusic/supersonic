package com.tencent.supersonic.headless.core.translator.calcite.s2sql;

import lombok.Data;


@Data
public class DimensionTimeTypeParams {

    private String isPrimary;

    private String timeGranularity;
}
