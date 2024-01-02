package com.tencent.supersonic.headless.server.pojo.yaml;

import com.tencent.supersonic.headless.api.enums.ModelSourceType;
import lombok.Data;

import java.util.List;


@Data
public class DataModelYamlTpl {

    private String name;

    private Long sourceId;

    private String sqlQuery;

    private String tableQuery;

    private List<IdentifyYamlTpl> identifiers;

    private List<DimensionYamlTpl> dimensions;

    private List<MeasureYamlTpl> measures;

    private ModelSourceType modelSourceTypeEnum;



}
