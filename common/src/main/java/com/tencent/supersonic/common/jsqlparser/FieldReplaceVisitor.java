package com.tencent.supersonic.common.jsqlparser;

import com.tencent.supersonic.common.util.ContextUtils;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.schema.Column;

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
        ReplaceService replaceService = ContextUtils.getBean(ReplaceService.class);
        replaceService.replaceColumn(column, fieldNameMap, exactReplace.get());
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
}
