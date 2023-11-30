package com.tencent.supersonic.common.util.jsqlparser;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

public class SqlNumberCorrecteBase extends SqlCorrecteBase {

    public Expression distinguishNumberCondition(Expression leftExpression, Expression expression) throws Exception {
        if (leftExpression instanceof LongValue) {
            return null;
        } else {
            return expression;
        }
    }

}
