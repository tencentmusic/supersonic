package com.tencent.supersonic.semantic.query.domain.parser.dsl;

import lombok.Data;


@Data
public class DimensionTimeTypeParams {

    private String isPrimary;

    private String timeGranularity;
}
