package com.tencent.supersonic.common.util.jsqlparser;

import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;

public class AggregateFunctionVisitor extends ExpressionVisitorAdapter {

    private boolean hasAggregateFunction = false;

    public boolean hasAggregateFunction() {
        return hasAggregateFunction;
    }

    @Override
    public void visit(Function function) {
        super.visit(function);
        hasAggregateFunction = true;
    }
}