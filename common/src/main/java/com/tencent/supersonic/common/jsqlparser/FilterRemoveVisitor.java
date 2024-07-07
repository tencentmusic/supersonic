package com.tencent.supersonic.common.jsqlparser;

import java.util.List;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.schema.Column;
import org.apache.commons.lang3.StringUtils;

public class FilterRemoveVisitor extends ExpressionVisitorAdapter {

    private List<String> filedNames;

    public FilterRemoveVisitor(List<String> filedNames) {
        this.filedNames = filedNames;
    }

    private boolean isRemove(Expression leftExpression) {
        if (!(leftExpression instanceof Column)) {
            return false;
        }
        Column leftColumnName = (Column) leftExpression;
        String columnName = leftColumnName.getColumnName();
        if (StringUtils.isEmpty(columnName)) {
            return false;
        }
        if (!filedNames.contains(columnName)) {
            return false;
        }
        return true;
    }

    @Override
    public void visit(EqualsTo expr) {
        if (!isRemove(expr.getLeftExpression())) {
            return;
        }
        expr.setRightExpression(new LongValue(1L));
        expr.setLeftExpression(new LongValue(1L));
    }

    @Override
    public void visit(MinorThan expr) {
        if (!isRemove(expr.getLeftExpression())) {
            return;
        }
        expr.setRightExpression(new LongValue(1L));
        expr.setLeftExpression(new LongValue(0L));
    }

    @Override
    public void visit(MinorThanEquals expr) {
        if (!isRemove(expr.getLeftExpression())) {
            return;
        }
        expr.setRightExpression(new LongValue(1L));
        expr.setLeftExpression(new LongValue(1L));
    }

    @Override
    public void visit(GreaterThan expr) {
        if (!isRemove(expr.getLeftExpression())) {
            return;
        }
        expr.setRightExpression(new LongValue(0L));
        expr.setLeftExpression(new LongValue(1L));
    }

    @Override
    public void visit(GreaterThanEquals expr) {
        if (!isRemove(expr.getLeftExpression())) {
            return;
        }
        expr.setRightExpression(new LongValue(1L));
        expr.setLeftExpression(new LongValue(1L));
    }

    @Override
    public void visit(InExpression expr) {
        if (!isRemove(expr.getLeftExpression())) {
            return;
        }
        expr.setNot(false);
        expr.setRightExpression(new LongValue(1L));
        expr.setLeftExpression(new LongValue(1L));
    }

}
