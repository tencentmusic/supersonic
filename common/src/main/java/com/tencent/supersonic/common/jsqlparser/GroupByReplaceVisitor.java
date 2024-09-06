package com.tencent.supersonic.common.jsqlparser;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.GroupByVisitor;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

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
            String columnName = getColumnName(expression);

            String replaceColumn =
                    parseVisitorHelper.getReplaceValue(columnName, fieldNameMap, exactReplace);
            if (StringUtils.isNotEmpty(replaceColumn)) {
                replaceExpression(groupByExpressions, i, expression, replaceColumn);
            }
        }
    }

    private String getColumnName(Expression expression) {
        if (expression instanceof Function) {
            Function function = (Function) expression;
            if (Objects.nonNull(function.getParameters().getExpressions().get(0))) {
                return function.getParameters().getExpressions().get(0).toString();
            }
        }
        return expression.toString();
    }

    private void replaceExpression(
            List<Expression> groupByExpressions,
            int index,
            Expression expression,
            String replaceColumn) {
        if (expression instanceof Column) {
            groupByExpressions.set(index, new Column(replaceColumn));
        } else if (expression instanceof Function) {
            try {
                Expression newExpression = CCJSqlParserUtil.parseExpression(replaceColumn);
                ExpressionList<Expression> newExpressionList = new ExpressionList<>();
                newExpressionList.add(newExpression);

                Function function = (Function) expression;
                if (function.getParameters().size() > 1) {
                    function.getParameters().stream()
                            .skip(1)
                            .forEach(e -> newExpressionList.add((Function) e));
                }
                function.setParameters(newExpressionList);
            } catch (JSQLParserException e) {
                log.error("Error parsing expression: {}", replaceColumn, e);
            }
        }
    }
}
