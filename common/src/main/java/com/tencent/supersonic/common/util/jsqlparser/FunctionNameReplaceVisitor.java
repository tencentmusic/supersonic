package com.tencent.supersonic.common.util.jsqlparser;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class FunctionNameReplaceVisitor extends ExpressionVisitorAdapter {

    private Map<String, String> functionMap;

    public FunctionNameReplaceVisitor(Map<String, String> functionMap) {
        this.functionMap = functionMap;
    }

    public void visit(Function function) {
        String replaceFunctionName = functionMap.get(function.getName().toLowerCase());
        if (StringUtils.isNotBlank(replaceFunctionName)) {
            function.setName(replaceFunctionName);
        }
    }

}