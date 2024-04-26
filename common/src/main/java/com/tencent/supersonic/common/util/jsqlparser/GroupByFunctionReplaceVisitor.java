package com.tencent.supersonic.common.util.jsqlparser;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
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
    private Map<String, UnaryOperator> functionCallMap;

    public GroupByFunctionReplaceVisitor(Map<String, String> functionMap) {
        this.functionMap = functionMap;
    }

    public GroupByFunctionReplaceVisitor(Map<String, String> functionMap, Map<String, UnaryOperator> functionCallMap) {
        this.functionMap = functionMap;
        this.functionCallMap = functionCallMap;
    }

    public void visit(GroupByElement groupByElement) {
        groupByElement.getGroupByExpressionList();
        ExpressionList groupByExpressionList = groupByElement.getGroupByExpressionList();
        List<Expression> groupByExpressions = groupByExpressionList.getExpressions();

        for (int i = 0; i < groupByExpressions.size(); i++) {
            Expression expression = groupByExpressions.get(i);
            if (expression instanceof Function) {
                Function function = (Function) expression;
                String functionName = function.getName().toLowerCase();
                String replaceName = functionMap.get(functionName);
                if (StringUtils.isNotBlank(replaceName)) {
                    function.setName(replaceName);
                    if (Objects.nonNull(functionCallMap) && functionCallMap.containsKey(functionName)) {
                        Object ret = functionCallMap.get(functionName).apply(function.getParameters());
                        if (Objects.nonNull(ret) && ret instanceof ExpressionList) {
                            ExpressionList expressionList = (ExpressionList) ret;
                            function.setParameters(expressionList);
                        }
                    }
                }
            }
        }
    }
}