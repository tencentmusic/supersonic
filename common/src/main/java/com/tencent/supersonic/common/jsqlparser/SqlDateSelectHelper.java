package com.tencent.supersonic.common.jsqlparser;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.select.PlainSelect;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Date field parsing helper class */
@Slf4j
public class SqlDateSelectHelper {

    public static DateVisitor.DateBoundInfo getDateBoundInfo(String sql, String dateField) {
        List<PlainSelect> plainSelectList = SqlSelectHelper.getPlainSelect(sql);
        if (plainSelectList.size() != 1) {
            return null;
        }
        PlainSelect plainSelect = plainSelectList.get(0);
        if (Objects.isNull(plainSelect)) {
            return null;
        }
        Expression where = plainSelect.getWhere();
        if (Objects.isNull(where)) {
            return null;
        }
        DateVisitor dateVisitor = new DateVisitor(Collections.singletonList(dateField));
        where.accept(dateVisitor);
        return dateVisitor.getDateBoundInfo();
    }
}
