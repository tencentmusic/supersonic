package com.tencent.supersonic.headless.server.pojo.yaml;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class MeasureYamlTpl {

    private String name;

    private String agg;

    private String expr;

    private String constraint;

    private String alias;

    private String createMetric;

}
