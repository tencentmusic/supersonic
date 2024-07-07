package com.tencent.supersonic.headless.core.translator.calcite.sql;


import com.tencent.supersonic.headless.core.translator.calcite.schema.SemanticSchema;


public interface Optimization {

    public void visit(SemanticSchema semanticSchema);
}
