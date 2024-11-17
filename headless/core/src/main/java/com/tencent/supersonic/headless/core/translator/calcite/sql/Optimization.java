package com.tencent.supersonic.headless.core.translator.calcite.sql;

import com.tencent.supersonic.headless.core.translator.calcite.schema.S2SemanticSchema;

public interface Optimization {

    public void visit(S2SemanticSchema semanticSchema);
}
