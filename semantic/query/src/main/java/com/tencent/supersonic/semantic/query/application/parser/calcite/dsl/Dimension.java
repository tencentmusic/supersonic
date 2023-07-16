package com.tencent.supersonic.semantic.query.application.parser.calcite.dsl;


import com.tencent.supersonic.semantic.query.application.parser.calcite.schema.SemanticItem;
import lombok.Data;


@Data
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
