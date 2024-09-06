package com.tencent.supersonic.headless.chat.corrector;

import com.tencent.supersonic.common.pojo.enums.DatePeriodEnum;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.pojo.enums.TimeMode;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.TimeDefaultConfig;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class S2SqlDateHelper {

    public static String getReferenceDate(ChatQueryContext chatQueryContext, Long dataSetId) {
        String defaultDate = DateUtils.getBeforeDate(0);
        if (Objects.isNull(dataSetId)) {
            return defaultDate;
        }
        DataSetSchema dataSetSchema =
                chatQueryContext.getSemanticSchema().getDataSetSchemaMap().get(dataSetId);
        if (dataSetSchema == null || dataSetSchema.getTagTypeTimeDefaultConfig() == null) {
            return defaultDate;
        }
        TimeDefaultConfig tagTypeTimeDefaultConfig = dataSetSchema.getTagTypeTimeDefaultConfig();
        String partitionTimeFormat = dataSetSchema.getPartitionTimeFormat();
        return getDefaultDate(defaultDate, tagTypeTimeDefaultConfig, partitionTimeFormat).getLeft();
    }

    public static Pair<String, String> getStartEndDate(
            ChatQueryContext chatQueryContext, Long dataSetId, QueryType queryType) {
        String defaultDate = DateUtils.getBeforeDate(0);
        if (Objects.isNull(dataSetId)) {
            return Pair.of(defaultDate, defaultDate);
        }
        DataSetSchema dataSetSchema =
                chatQueryContext.getSemanticSchema().getDataSetSchemaMap().get(dataSetId);
        if (Objects.isNull(dataSetSchema)) {
            return Pair.of(defaultDate, defaultDate);
        }
        TimeDefaultConfig defaultConfig = dataSetSchema.getMetricTypeTimeDefaultConfig();
        if (QueryType.DETAIL.equals(queryType) && defaultConfig.getUnit() >= 0) {
            defaultConfig = dataSetSchema.getTagTypeTimeDefaultConfig();
        }
        String partitionTimeFormat = dataSetSchema.getPartitionTimeFormat();
        return getDefaultDate(defaultDate, defaultConfig, partitionTimeFormat);
    }

    private static Pair<String, String> getDefaultDate(
            String defaultDate, TimeDefaultConfig defaultConfig, String partitionTimeFormat) {
        if (defaultConfig == null) {
            return Pair.of(null, null);
        }
        Integer unit = defaultConfig.getUnit();
        if (unit == null) {
            return Pair.of(defaultDate, defaultDate);
        }

        // If the unit is set to less than 0, then do not add relative date.
        if (unit < 0) {
            return Pair.of(null, null);
        }

        String period = defaultConfig.getPeriod();
        TimeMode timeMode = defaultConfig.getTimeMode();
        DatePeriodEnum datePeriodEnum = DatePeriodEnum.get(period);

        String startDate = DateUtils.getBeforeDate(unit, datePeriodEnum);
        String endDate = DateUtils.getBeforeDate(0, DatePeriodEnum.DAY);

        if (unit == 0 || TimeMode.LAST.equals(timeMode)) {
            endDate = startDate;
        }
        if (StringUtils.isNotBlank(partitionTimeFormat)) {
            startDate = formatDate(startDate, partitionTimeFormat);
            endDate = formatDate(endDate, partitionTimeFormat);
        }
        return Pair.of(startDate, endDate);
    }

    private static String formatDate(String dateStr, String format) {
        try {
            // Assuming the input date format is "yyyy-MM-dd"
            SimpleDateFormat inputFormat = new SimpleDateFormat(DateUtils.DATE_FORMAT);
            Date date = inputFormat.parse(dateStr);
            SimpleDateFormat outputFormat = new SimpleDateFormat(format);
            return outputFormat.format(date);
        } catch (Exception e) {
            // Handle the exception, maybe log it and return the original dateStr
            return dateStr;
        }
    }
}
