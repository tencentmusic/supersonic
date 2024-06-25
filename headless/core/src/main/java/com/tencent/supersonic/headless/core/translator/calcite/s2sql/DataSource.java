package com.tencent.supersonic.headless.core.translator.calcite.s2sql;

import lombok.Builder;
import lombok.Data;

import java.util.List;


@Data
@Builder
public class DataSource {

    private Long id;

    private String name;

    private Long sourceId;

    private String type;

    private String sqlQuery;

    private String tableQuery;

    private List<Identify> identifiers;

    private List<Dimension> dimensions;

    private List<Measure> measures;

    private String aggTime;

    private Materialization.TimePartType timePartType = Materialization.TimePartType.None;
}
