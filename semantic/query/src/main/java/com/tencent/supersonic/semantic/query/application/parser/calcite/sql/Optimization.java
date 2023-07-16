package com.tencent.supersonic.semantic.query.application.parser.calcite.sql;


import com.tencent.supersonic.semantic.query.application.parser.calcite.sql.node.SemanticNode;

public interface Optimization {

    public void visit(SemanticNode semanticNode);
}
