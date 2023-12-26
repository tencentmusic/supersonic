package com.tencent.supersonic.headless.query.parser.calcite.sql;


import com.tencent.supersonic.headless.query.parser.calcite.schema.HeadlessSchema;


public interface Optimization {

    public void visit(HeadlessSchema headlessSchema);
}
