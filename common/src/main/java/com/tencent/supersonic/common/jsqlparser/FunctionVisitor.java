package com.tencent.supersonic.common.jsqlparser;

import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;

import java.util.HashSet;
import java.util.Set;

public class FunctionVisitor extends ExpressionVisitorAdapter {

    private Set<String> functionNames = new HashSet<>();

    public Set<String> getFunctionNames() {
        return functionNames;
    }

    @Override
    public void visit(Function function) {
        super.visit(function);
        functionNames.add(function.getName());
    }
}
