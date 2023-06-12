package com.tencent.supersonic.semantic.core.domain.pojo;


import com.tencent.supersonic.common.pojo.SchemaItem;
import lombok.Data;


@Data
public class Dimension extends SchemaItem {

    //categorical time
    private String type;

    private String expr;

    private Long domainId;


    private Long datasourceId;

    private String semanticType;

}
