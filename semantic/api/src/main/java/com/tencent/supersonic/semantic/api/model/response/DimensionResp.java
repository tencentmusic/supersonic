package com.tencent.supersonic.semantic.api.model.response;


import com.tencent.supersonic.semantic.api.model.pojo.DimValueMap;
import com.tencent.supersonic.semantic.api.model.pojo.SchemaItem;

import java.util.List;

import lombok.Data;
import lombok.ToString;


@Data
@ToString(callSuper = true)
public class DimensionResp extends SchemaItem {

    private Long modelId;

    private String type;

    private String expr;

    private String fullPath;

    private Long datasourceId;

    private String datasourceName;

    private String datasourceBizName;
    //DATE ID CATEGORY
    private String semanticType;

    private String alias;

    private List<String> defaultValues;

    private List<DimValueMap> dimValueMaps;

}
