package com.tencent.supersonic.common.util.jsqlparser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.schema.Column;

@Slf4j
public class FieldReplaceVisitor extends ExpressionVisitorAdapter {

    ParseVisitorHelper parseVisitorHelper = new ParseVisitorHelper();

    private Map<String, String> fieldToBizName;
    private List<Expression> waitingForAdds = new ArrayList<>();

    public FieldReplaceVisitor(Map<String, String> fieldToBizName) {
        this.fieldToBizName = fieldToBizName;
    }

    @Override
    public void visit(Column column) {
        parseVisitorHelper.replaceColumn(column, fieldToBizName);
    }

    @Override
    public void visit(MinorThan expr) {
        Expression expression = parseVisitorHelper.reparseDate(expr, fieldToBizName, ">");
        if (Objects.nonNull(expression)) {
            waitingForAdds.add(expression);
        }
    }

    @Override
    public void visit(MinorThanEquals expr) {
        Expression expression = parseVisitorHelper.reparseDate(expr, fieldToBizName, ">=");
        if (Objects.nonNull(expression)) {
            waitingForAdds.add(expression);
        }
    }


    @Override
    public void visit(GreaterThan expr) {
        Expression expression = parseVisitorHelper.reparseDate(expr, fieldToBizName, "<");
        if (Objects.nonNull(expression)) {
            waitingForAdds.add(expression);
        }
    }

    @Override
    public void visit(GreaterThanEquals expr) {
        Expression expression = parseVisitorHelper.reparseDate(expr, fieldToBizName, "<=");
        if (Objects.nonNull(expression)) {
            waitingForAdds.add(expression);
        }
    }

    public List<Expression> getWaitingForAdds() {
        return waitingForAdds;
    }

}