package com.tencent.supersonic.common.jsqlparser;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

@Slf4j
public class FunctionNameReplaceVisitor extends ExpressionVisitorAdapter {

    private Map<String, String> functionMap;
    private Map<String, UnaryOperator> functionCallMap;

    public FunctionNameReplaceVisitor(Map<String, String> functionMap,
            Map<String, UnaryOperator> functionCallMap) {
        this.functionMap = functionMap;
        this.functionCallMap = functionCallMap;
    }

    public void visit(Function function) {
        String functionName = function.getName().toLowerCase();
        String replaceFunctionName = functionMap.get(functionName);
        if (StringUtils.isNotBlank(replaceFunctionName)) {
            function.setName(replaceFunctionName);
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
