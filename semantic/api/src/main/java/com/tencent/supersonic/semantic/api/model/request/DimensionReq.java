package com.tencent.supersonic.semantic.api.model.request;

import com.tencent.supersonic.semantic.api.model.pojo.DimValueMap;
import com.tencent.supersonic.semantic.api.model.pojo.SchemaItem;

import javax.validation.constraints.NotNull;

import lombok.Data;

import java.util.List;

@Data
public class DimensionReq extends SchemaItem {

    private Long modelId;

    private String type;

    @NotNull(message = "expr can not be null")
    private String expr;


    private Long datasourceId;

    //DATE ID CATEGORY
    private String semanticType = "CATEGORY";

    private String alias;

    private List<String> defaultValues;

    private List<DimValueMap> dimValueMaps;
}
