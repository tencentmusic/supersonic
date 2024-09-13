package com.tencent.supersonic.common.jsqlparser;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.statement.select.GroupByElement;

@Slf4j
public class GroupByVisitor implements net.sf.jsqlparser.statement.select.GroupByVisitor {

    private boolean hasAggregateFunction = false;

    public void visit(GroupByElement groupByElement) {
        this.hasAggregateFunction = true;
    }

    public boolean isHasAggregateFunction() {
        return hasAggregateFunction;
    }
}
