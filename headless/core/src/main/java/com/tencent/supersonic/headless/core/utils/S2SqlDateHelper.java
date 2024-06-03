package com.tencent.supersonic.headless.core.utils;

import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.pojo.enums.TimeMode;
import com.tencent.supersonic.common.util.DatePeriodEnum;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.TimeDefaultConfig;
import com.tencent.supersonic.headless.core.pojo.QueryContext;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Objects;

public class S2SqlDateHelper {

    public static String getReferenceDate(QueryContext queryContext, Long dataSetId) {
        String defaultDate = DateUtils.getBeforeDate(0);
        if (Objects.isNull(dataSetId)) {
            return defaultDate;
        }
        DataSetSchema dataSetSchema = queryContext.getSemanticSchema().getDataSetSchemaMap().get(dataSetId);
        if (dataSetSchema == null || dataSetSchema.getTagTypeTimeDefaultConfig() == null) {
            return defaultDate;
        }
        TimeDefaultConfig tagTypeTimeDefaultConfig = dataSetSchema.getTagTypeTimeDefaultConfig();
        return getDefaultDate(defaultDate, tagTypeTimeDefaultConfig).getLeft();
    }

    public static Pair<String, String> getStartEndDate(QueryContext queryContext, Long dataSetId,
            QueryType queryType) {
        String defaultDate = DateUtils.getBeforeDate(0);
        if (Objects.isNull(dataSetId)) {
            return Pair.of(defaultDate, defaultDate);
        }
        DataSetSchema dataSetSchema = queryContext.getSemanticSchema().getDataSetSchemaMap().get(dataSetId);
        if (dataSetSchema == null) {
            return Pair.of(defaultDate, defaultDate);
        }
        TimeDefaultConfig defaultConfig = dataSetSchema.getMetricTypeTimeDefaultConfig();
        if (QueryType.DETAIL.equals(queryType)) {
            defaultConfig = dataSetSchema.getTagTypeTimeDefaultConfig();
        }
        return getDefaultDate(defaultDate, defaultConfig);
    }

    private static Pair<String, String> getDefaultDate(String defaultDate, TimeDefaultConfig defaultConfig) {
        if (Objects.isNull(defaultConfig)) {
            return Pair.of(null, null);
        }
        Integer unit = defaultConfig.getUnit();
        String period = defaultConfig.getPeriod();
        TimeMode timeMode = defaultConfig.getTimeMode();
        if (Objects.nonNull(unit)) {
            // If the unit is set to less than 0, then do not add relative date.
            if (unit < 0) {
                return Pair.of(null, null);
            }
            DatePeriodEnum datePeriodEnum = DatePeriodEnum.get(period);
            String startDate = DateUtils.getBeforeDate(unit, datePeriodEnum);
            String endDate = DateUtils.getBeforeDate(1, datePeriodEnum);
            if (unit == 0) {
                endDate = startDate;
            }
            if (TimeMode.LAST.equals(timeMode)) {
                endDate = startDate;
            }
            return Pair.of(startDate, endDate);
        }
        return Pair.of(defaultDate, defaultDate);
    }

}
