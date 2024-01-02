package com.tencent.supersonic.headless.core.parser.calcite.sql;


import com.tencent.supersonic.headless.core.parser.calcite.schema.HeadlessSchema;


public interface Optimization {

    public void visit(HeadlessSchema headlessSchema);
}
