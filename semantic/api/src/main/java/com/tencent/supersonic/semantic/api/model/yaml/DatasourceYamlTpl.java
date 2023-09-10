package com.tencent.supersonic.semantic.api.model.yaml;

import java.util.List;
import lombok.Data;


@Data
public class DatasourceYamlTpl {

    private String name;

    private Long sourceId;

    private String sqlQuery;

    private String tableQuery;

    private List<IdentifyYamlTpl> identifiers;

    private List<DimensionYamlTpl> dimensions;

    private List<MeasureYamlTpl> measures;



}
