package com.tencent.supersonic.chat.core.parser.sql.llm;

import com.tencent.supersonic.chat.api.pojo.ViewSchema;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.common.util.DatePeriodEnum;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.headless.api.pojo.TimeDefaultConfig;

import java.util.Objects;

public class S2SqlDateHelper {

    public static String getReferenceDate(QueryContext queryContext, Long viewId) {
        String defaultDate = DateUtils.getBeforeDate(0);
        if (Objects.isNull(viewId)) {
            return defaultDate;
        }
        ViewSchema viewSchema = queryContext.getSemanticSchema().getViewSchemaMap().get(viewId);
        if (viewSchema == null || viewSchema.getTagTypeTimeDefaultConfig() == null) {
            return defaultDate;
        }
        TimeDefaultConfig tagTypeTimeDefaultConfig = viewSchema.getTagTypeTimeDefaultConfig();
        Integer unit = tagTypeTimeDefaultConfig.getUnit();
        String period = tagTypeTimeDefaultConfig.getPeriod();
        if (Objects.nonNull(unit)) {
            // If the unit is set to less than 0, then do not add relative date.
            if (unit < 0) {
                return null;
            }
            DatePeriodEnum datePeriodEnum = DatePeriodEnum.get(period);
            if (Objects.isNull(datePeriodEnum)) {
                return DateUtils.getBeforeDate(unit);
            } else {
                return DateUtils.getBeforeDate(unit, datePeriodEnum);
            }
        }
        return defaultDate;
    }

}
