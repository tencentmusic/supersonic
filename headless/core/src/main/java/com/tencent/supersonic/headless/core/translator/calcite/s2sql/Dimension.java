package com.tencent.supersonic.headless.core.translator.calcite.s2sql;

import com.tencent.supersonic.headless.core.translator.calcite.schema.SemanticItem;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class Dimension implements SemanticItem {

    String name;
    private String owners;
    private String type;
    private String expr;
    private DimensionTimeTypeParams dimensionTimeTypeParams;
    private DataType dataType = DataType.UNKNOWN;
    private String bizName;
    private List<String> defaultValues;
    private Map<String, Object> ext;

    @Override
    public String getName() {
        return name;
    }
}
