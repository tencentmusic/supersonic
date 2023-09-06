package com.tencent.supersonic.semantic.model.domain.pojo;


import com.tencent.supersonic.semantic.api.model.pojo.DimValueMap;
import com.tencent.supersonic.semantic.api.model.pojo.SchemaItem;
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

}
