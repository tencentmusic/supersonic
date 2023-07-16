package com.tencent.supersonic.semantic.query.application.parser.calcite.dsl;

import java.util.List;
import lombok.Data;


@Data
public class DataSource {

    private String name;

    private Long sourceId;

    private String sqlQuery;

    private String tableQuery;

    private List<Identify> identifiers;

    private List<Dimension> dimensions;

    private List<Measure> measures;
}
