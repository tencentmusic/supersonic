package com.tencent.supersonic.headless.common.model.yaml;

import com.tencent.supersonic.headless.common.model.enums.ModelSourceTypeEnum;
import java.util.List;
import lombok.Data;


@Data
public class DataModelYamlTpl {

    private String name;

    private Long sourceId;

    private String sqlQuery;

    private String tableQuery;

    private List<IdentifyYamlTpl> identifiers;

    private List<DimensionYamlTpl> dimensions;

    private List<MeasureYamlTpl> measures;

    private ModelSourceTypeEnum modelSourceTypeEnum;



}
