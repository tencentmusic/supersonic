package com.tencent.supersonic.headless.core.parser.calcite.sql;


import com.tencent.supersonic.headless.core.parser.calcite.schema.SemanticSchema;


public interface Optimization {

    public void visit(SemanticSchema semanticSchema);
}
