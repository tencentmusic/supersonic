package com.tencent.supersonic.semantic.query.parser.calcite.dsl;


import com.tencent.supersonic.semantic.query.parser.calcite.schema.SemanticItem;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class Dimension implements SemanticItem {

    String name;
    private String owners;
    private String type;
    private String expr;
    private DimensionTimeTypeParams dimensionTimeTypeParams;

    @Override
    public String getName() {
        return name;
    }
}
