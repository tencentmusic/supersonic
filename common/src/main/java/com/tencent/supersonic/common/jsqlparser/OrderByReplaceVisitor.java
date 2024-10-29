package com.tencent.supersonic.common.jsqlparser;

import com.tencent.supersonic.common.util.ContextUtils;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.OrderByVisitorAdapter;

import java.util.Map;

public class OrderByReplaceVisitor extends OrderByVisitorAdapter {
    private Map<String, String> fieldNameMap;
    private boolean exactReplace;

    public OrderByReplaceVisitor(Map<String, String> fieldNameMap, boolean exactReplace) {
        this.fieldNameMap = fieldNameMap;
        this.exactReplace = exactReplace;
    }

    @Override
    public void visit(OrderByElement orderBy) {
        Expression expression = orderBy.getExpression();
        ReplaceService replaceService = ContextUtils.getBean(ReplaceService.class);
        if (expression instanceof Column) {
            replaceService.replaceColumn((Column) expression, fieldNameMap, exactReplace);
        }
        if (expression instanceof Function) {
            replaceService.replaceFunction((Function) expression, fieldNameMap, exactReplace);
        }
        super.visit(orderBy);
    }

}
