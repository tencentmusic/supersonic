package com.tencent.supersonic.common.util.jsqlparser;

import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.jsqlparser.DateVisitor.DateBoundInfo;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.select.PlainSelect;

/**
 * Date field parsing helper class
 */
@Slf4j
public class SqlDateSelectHelper {

    public static DateBoundInfo getDateBoundInfo(String sql) {
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
        DateVisitor dateVisitor = new DateVisitor(TimeDimensionEnum.getChNameList());
        where.accept(dateVisitor);
        return dateVisitor.getDateBoundInfo();
    }
}

