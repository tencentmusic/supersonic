package com.tencent.supersonic.common.util.jsqlparser;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;

import java.util.Objects;

@Slf4j
public abstract class SqlCorrecteBase {
    public Expression filteredWhereExpression(Expression where) throws Exception {
        if (Objects.isNull(where)) {
            return null;
        }
        if (where instanceof Parenthesis) {
            Expression expression = filteredWhereExpression(((Parenthesis) where).getExpression());
            if (expression != null) {
                try {
                    Expression parseExpression = CCJSqlParserUtil.parseExpression("(" + expression + ")");
                    return parseExpression;
                } catch (JSQLParserException jsqlParserException) {
                    log.info("jsqlParser has an exception:{}", jsqlParserException.toString());
                }
            } else {
                return expression;
            }
        } else if (where instanceof AndExpression) {
            AndExpression andExpression = (AndExpression) where;
            return filteredNumberExpression(andExpression);
        } else if (where instanceof OrExpression) {
            OrExpression orExpression = (OrExpression) where;
            return filteredNumberExpression(orExpression);
        } else {
            return replaceComparisonOperatorFunction(where);
        }
        return where;
    }

    private <T extends BinaryExpression> Expression filteredNumberExpression(T binaryExpression) throws Exception{
        Expression leftExpression = filteredWhereExpression(binaryExpression.getLeftExpression());
        Expression rightExpression = filteredWhereExpression(binaryExpression.getRightExpression());
        if (leftExpression != null && rightExpression != null) {
            binaryExpression.setLeftExpression(leftExpression);
            binaryExpression.setRightExpression(rightExpression);
            return binaryExpression;
        } else if (leftExpression != null && rightExpression == null) {
            return leftExpression;
        } else if (leftExpression == null && rightExpression != null) {
            return rightExpression;
        } else {
            return null;
        }
    }

    private Expression replaceComparisonOperatorFunction(Expression expression) throws Exception{
        if (Objects.isNull(expression)) {
            return null;
        }
        if (expression instanceof GreaterThanEquals) {
            return removeSingleFilter((GreaterThanEquals) expression);
        } else if (expression instanceof GreaterThan) {
            return removeSingleFilter((GreaterThan) expression);
        } else if (expression instanceof MinorThan) {
            return removeSingleFilter((MinorThan) expression);
        } else if (expression instanceof MinorThanEquals) {
            return removeSingleFilter((MinorThanEquals) expression);
        } else if (expression instanceof EqualsTo) {
            return removeSingleFilter((EqualsTo) expression);
        } else if (expression instanceof NotEqualsTo) {
            return removeSingleFilter((NotEqualsTo) expression);
        } else if (expression instanceof InExpression) {
            InExpression inExpression = (InExpression) expression;
            Expression leftExpression = inExpression.getLeftExpression();
            return distinguishNumberCondition(leftExpression, expression);
        } else if (expression instanceof LikeExpression) {
            LikeExpression likeExpression = (LikeExpression) expression;
            Expression leftExpression = likeExpression.getLeftExpression();
            return distinguishNumberCondition(leftExpression, expression);
        }
        return expression;
    }

    private <T extends ComparisonOperator> Expression removeSingleFilter(T comparisonExpression) throws Exception{
        Expression leftExpression = comparisonExpression.getLeftExpression();
        return distinguishNumberCondition(leftExpression, comparisonExpression);
    }

    public abstract Expression distinguishNumberCondition(Expression leftExpression, Expression expression) throws Exception;
}
