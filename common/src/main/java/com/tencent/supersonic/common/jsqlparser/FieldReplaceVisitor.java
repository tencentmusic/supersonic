package com.tencent.supersonic.common.jsqlparser;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.AnalyticExpression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.WindowDefinition;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.OrderByElement;
import org.springframework.util.CollectionUtils;

import java.util.Map;

@Slf4j
public class FieldReplaceVisitor extends ExpressionVisitorAdapter {

    private Map<String, String> fieldNameMap;
    private ThreadLocal<Boolean> exactReplace = ThreadLocal.withInitial(() -> false);

    public FieldReplaceVisitor(Map<String, String> fieldNameMap, boolean exactReplace) {
        this.fieldNameMap = fieldNameMap;
        this.exactReplace.set(exactReplace);
    }

    @Override
    public void visit(Column column) {
        SqlReplaceHelper.replaceColumn(column, fieldNameMap, exactReplace.get());
    }

    @Override
    public void visit(Function function) {
        boolean originalExactReplace = exactReplace.get();
        exactReplace.set(true);
        try {
            super.visit(function);
        } finally {
            exactReplace.set(originalExactReplace);
        }
    }

    @Override
    public void visit(AnalyticExpression expr) {
        super.visit(expr);
        WindowDefinition windowDefinition = expr.getWindowDefinition();
        if (windowDefinition != null
                && !CollectionUtils.isEmpty(windowDefinition.getOrderByElements())) {
            for (OrderByElement element : windowDefinition.getOrderByElements()) {
                element.getExpression().accept(this);
            }
        }
    }
}
