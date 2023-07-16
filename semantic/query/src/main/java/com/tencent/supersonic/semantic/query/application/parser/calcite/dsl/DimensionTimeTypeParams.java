package com.tencent.supersonic.semantic.query.application.parser.calcite.dsl;

import lombok.Data;


@Data
public class DimensionTimeTypeParams {

    private String isPrimary;

    private String timeGranularity;
}
