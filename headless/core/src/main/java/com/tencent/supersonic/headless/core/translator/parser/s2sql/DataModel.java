package com.tencent.supersonic.headless.core.translator.parser.s2sql;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DataModel {

    private Long id;

    private String name;

    private Long modelId;

    private String type;

    private String sqlQuery;

    private String tableQuery;

    private List<Identify> identifiers;

    private List<Dimension> dimensions;

    private List<Measure> measures;

    private String aggTime;

    private Materialization.TimePartType timePartType = Materialization.TimePartType.None;
}
