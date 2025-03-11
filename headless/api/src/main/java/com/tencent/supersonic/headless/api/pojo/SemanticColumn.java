package com.tencent.supersonic.headless.api.pojo;

import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.headless.api.pojo.enums.FieldType;
import lombok.Data;

@Data
public class SemanticColumn {

    private String columnName;

    private String dataType;

    private String comment;

    private FieldType filedType;

    private AggOperatorEnum agg = AggOperatorEnum.NONE;

    private String name;

    private String expr;

    private String unit;

}
