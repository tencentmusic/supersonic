package com.tencent.supersonic.semantic.query.domain.parser.dsl;


import com.tencent.supersonic.semantic.query.domain.parser.schema.SemanticItem;
import lombok.Data;


@Data
public class Dimension implements SemanticItem {

    String name;

    @Override
    public String getName() {
        return name;
    }


    private String owners;

    private String type;

    private String expr;

    private DimensionTimeTypeParams dimensionTimeTypeParams;
}
