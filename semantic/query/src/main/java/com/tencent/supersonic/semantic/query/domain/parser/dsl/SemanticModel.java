package com.tencent.supersonic.semantic.query.domain.parser.dsl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class SemanticModel {
    private String rootPath;
    private List<Metric> metrics = new ArrayList<>();
    private Map<String, DataSource> datasourceMap = new HashMap<>();
    private Map<String, List<Dimension>> dimensionMap = new HashMap<>();
}
