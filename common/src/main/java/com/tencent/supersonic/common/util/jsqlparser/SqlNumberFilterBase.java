package com.tencent.supersonic.common.util.jsqlparser;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
//mainly remove number filter in where and having,such as: 2>1
public class SqlNumberFilterBase extends SqlEditBase {

    public Expression distinguishNumberCondition(Expression leftExpression, Expression expression) throws Exception {
        if (leftExpression instanceof LongValue) {
            return null;
        } else {
            return expression;
        }
    }

}
