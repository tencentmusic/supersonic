package com.tencent.supersonic.semantic.query.domain.parser.convertor.sql;


import com.tencent.supersonic.semantic.query.domain.parser.convertor.sql.node.SemanticNode;

public interface Optimization {

    public void visit(SemanticNode semanticNode);
}
