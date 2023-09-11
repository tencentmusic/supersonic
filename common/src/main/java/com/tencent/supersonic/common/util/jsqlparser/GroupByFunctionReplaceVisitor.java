package com.tencent.supersonic.common.util.jsqlparser;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.GroupByVisitor;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class GroupByFunctionReplaceVisitor implements GroupByVisitor {

    private Map<String, String> functionMap;

    public GroupByFunctionReplaceVisitor(Map<String, String> functionMap) {
        this.functionMap = functionMap;
    }

    public void visit(GroupByElement groupByElement) {
        groupByElement.getGroupByExpressionList();
        ExpressionList groupByExpressionList = groupByElement.getGroupByExpressionList();
        List<Expression> groupByExpressions = groupByExpressionList.getExpressions();

        for (int i = 0; i < groupByExpressions.size(); i++) {
            Expression expression = groupByExpressions.get(i);
            if (expression instanceof Function) {
                Function function = (Function) expression;
                String replaceName = functionMap.get(function.getName().toLowerCase());
                if (StringUtils.isNotBlank(replaceName)) {
                    function.setName(replaceName);
                }
            }
        }
    }
}