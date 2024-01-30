package com.tencent.supersonic.headless.server.pojo.yaml;

import com.tencent.supersonic.headless.api.pojo.enums.ModelSourceType;
import com.tencent.supersonic.headless.api.pojo.Field;
import lombok.Data;

import java.util.List;


@Data
public class DataModelYamlTpl {

    private Long id;

    private String name;

    private Long sourceId;

    private String type;

    private String sqlQuery;

    private String tableQuery;

    private List<IdentifyYamlTpl> identifiers;

    private List<DimensionYamlTpl> dimensions;

    private List<MeasureYamlTpl> measures;

    private List<Field> fields;

    private ModelSourceType modelSourceTypeEnum;



}
