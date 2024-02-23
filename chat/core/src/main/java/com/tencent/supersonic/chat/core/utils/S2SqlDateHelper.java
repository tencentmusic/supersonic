package com.tencent.supersonic.chat.core.utils;

import com.tencent.supersonic.chat.api.pojo.ViewSchema;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.util.DatePeriodEnum;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.headless.api.pojo.TimeDefaultConfig;
import java.util.Objects;
import org.apache.commons.lang3.tuple.Pair;

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
        return getDefaultDate(defaultDate, tagTypeTimeDefaultConfig).getLeft();
    }

    public static Pair<String, String> getStartEndDate(QueryContext queryContext,
            Long viewId, QueryType queryType) {
        String defaultDate = DateUtils.getBeforeDate(0);
        if (Objects.isNull(viewId)) {
            return Pair.of(defaultDate, defaultDate);
        }
        ViewSchema viewSchema = queryContext.getSemanticSchema().getViewSchemaMap().get(viewId);
        if (viewSchema == null) {
            return Pair.of(defaultDate, defaultDate);
        }
        TimeDefaultConfig defaultConfig = viewSchema.getMetricTypeTimeDefaultConfig();
        if (QueryType.TAG.equals(queryType)) {
            defaultConfig = viewSchema.getTagTypeTimeDefaultConfig();
        }
        return getDefaultDate(defaultDate, defaultConfig);
    }

    private static Pair<String, String> getDefaultDate(String defaultDate, TimeDefaultConfig defaultConfig) {
        if (Objects.isNull(defaultConfig)) {
            return Pair.of(null, null);
        }
        Integer unit = defaultConfig.getUnit();
        String period = defaultConfig.getPeriod();
        if (Objects.nonNull(unit)) {
            // If the unit is set to less than 0, then do not add relative date.
            if (unit < 0) {
                return Pair.of(null, null);
            }
            DatePeriodEnum datePeriodEnum = DatePeriodEnum.get(period);
            if (Objects.isNull(datePeriodEnum)) {
                return Pair.of(DateUtils.getBeforeDate(unit), DateUtils.getBeforeDate(1));
            } else {
                return Pair.of(DateUtils.getBeforeDate(unit, datePeriodEnum),
                        DateUtils.getBeforeDate(1, datePeriodEnum));
            }
        }
        return Pair.of(defaultDate, defaultDate);
    }

}
