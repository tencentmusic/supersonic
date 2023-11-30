package com.tencent.supersonic.common.util.jsqlparser;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;

public class SqlDateCorrecteBase extends SqlCorrecteBase {
    public Expression distinguishNumberCondition(Expression leftExpression, Expression expression) throws Exception {
        if (leftExpression instanceof Function) {
            Function function = (Function) leftExpression;
            if (function.getName().equals(JsqlConstants.DATE_FUNCTION)) {
                return CCJSqlParserUtil.parseExpression("(发布日期 <= '2023-08-09' AND 发布日期 >= '2023-08-08')");
            }
            System.out.println(function.getName());
            return expression;
        } else {
            return expression;
        }
    }
}
