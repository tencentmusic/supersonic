package com.tencent.supersonic.semantic.api.core.response;


import com.tencent.supersonic.common.pojo.SchemaItem;
import lombok.Data;
import java.util.List;


@Data
public class DimensionResp extends SchemaItem {

    private Long domainId;

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


}
