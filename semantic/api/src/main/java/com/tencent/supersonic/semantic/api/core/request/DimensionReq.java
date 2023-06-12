package com.tencent.supersonic.semantic.api.core.request;

import com.tencent.supersonic.common.pojo.SchemaItem;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DimensionReq extends SchemaItem {

    private Long domainId;

    private String type;

    @NotNull(message = "expr can not be null")
    private String expr;


    private Long datasourceId;

    //DATE ID CATEGORY
    private String semanticType = "CATEGORY";


}
