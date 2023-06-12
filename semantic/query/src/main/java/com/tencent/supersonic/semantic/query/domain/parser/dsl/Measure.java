package com.tencent.supersonic.semantic.query.domain.parser.dsl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Measure {

    private String name;

    //sum max min avg count distinct
    private String agg;

    private String expr;

    private String constraint;

    private String alias;

    private String createMetric;
}
