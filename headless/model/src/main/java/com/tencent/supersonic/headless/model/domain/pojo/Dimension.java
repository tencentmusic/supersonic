package com.tencent.supersonic.headless.model.domain.pojo;


import com.tencent.supersonic.common.pojo.enums.DataTypeEnums;
import com.tencent.supersonic.headless.common.model.pojo.DimValueMap;
import com.tencent.supersonic.headless.common.model.pojo.SchemaItem;
import lombok.Data;

import java.util.List;

@Data
public class Dimension extends SchemaItem {

    //categorical time
    private String type;

    private String expr;

    private Long modelId;

    private Long datasourceId;

    private String semanticType;

    private String alias;

    private List<String> defaultValues;

    private List<DimValueMap> dimValueMaps;

    private DataTypeEnums dataType;

}
