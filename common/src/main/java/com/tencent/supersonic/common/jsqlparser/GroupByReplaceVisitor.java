package com.tencent.supersonic.common.jsqlparser;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.GroupByVisitor;

import java.util.List;
import java.util.Map;

@Slf4j
public class GroupByReplaceVisitor implements GroupByVisitor {

    ParseVisitorHelper parseVisitorHelper = new ParseVisitorHelper();
    private Map<String, String> fieldNameMap;
    private boolean exactReplace;

    public GroupByReplaceVisitor(Map<String, String> fieldNameMap, boolean exactReplace) {
        this.fieldNameMap = fieldNameMap;
        this.exactReplace = exactReplace;
    }

    public void visit(GroupByElement groupByElement) {
        ExpressionList groupByExpressionList = groupByElement.getGroupByExpressionList();
        List<Expression> groupByExpressions = groupByExpressionList.getExpressions();

        for (int i = 0; i < groupByExpressions.size(); i++) {
            Expression expression = groupByExpressions.get(i);
            replaceExpression(expression);
        }
    }

    private void replaceExpression(Expression expression) {
        if (expression instanceof Column) {
            parseVisitorHelper.replaceColumn((Column) expression, fieldNameMap, exactReplace);
        } else if (expression instanceof Function) {
            parseVisitorHelper.replaceFunction((Function) expression, fieldNameMap, exactReplace);
        }
    }
}
