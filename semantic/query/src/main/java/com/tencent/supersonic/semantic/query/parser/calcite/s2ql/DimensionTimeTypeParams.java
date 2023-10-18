package com.tencent.supersonic.semantic.query.parser.calcite.s2ql;

import lombok.Data;


@Data
public class DimensionTimeTypeParams {

    private String isPrimary;

    private String timeGranularity;
}
