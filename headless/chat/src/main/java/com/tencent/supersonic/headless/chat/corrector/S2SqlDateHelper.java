package com.tencent.supersonic.headless.chat.corrector;

import com.tencent.supersonic.common.pojo.enums.DatePeriodEnum;
import com.tencent.supersonic.common.pojo.enums.TimeMode;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.headless.api.pojo.TimeDefaultConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class S2SqlDateHelper {

    public static Pair<String, String> calculateDateRange(TimeDefaultConfig timeConfig,
            String timeFormat) {
        return calculateDateRange(DateUtils.getBeforeDate(0), timeConfig, timeFormat);
    }

    public static Pair<String, String> calculateDateRange(String currentDate,
            TimeDefaultConfig timeConfig, String timeFormat) {
        Integer unit = timeConfig.getUnit();
        if (timeConfig == null || unit == null || unit < 0) {
            return Pair.of(null, null);
        }

        TimeMode timeMode = timeConfig.getTimeMode();
        DatePeriodEnum datePeriod = timeConfig.getPeriod();
        String startDate;
        String endDate;
        switch (timeMode) {
            case CURRENT:
                startDate = DateUtils.getBeforeDate(currentDate, datePeriod);
                endDate = currentDate;
                break;
            case RECENT:
                startDate = DateUtils.getBeforeDate(currentDate, unit, datePeriod);
                endDate = currentDate;
                break;
            case LAST:
            default:
                startDate = DateUtils.getBeforeDate(currentDate, unit, datePeriod);
                endDate = DateUtils.getBeforeDate(currentDate, unit, datePeriod);
                break;
        }

        if (StringUtils.isNotBlank(timeFormat)) {
            startDate = reformatDate(startDate, timeFormat);
            endDate = reformatDate(endDate, timeFormat);
        }
        return Pair.of(startDate, endDate);
    }

    private static String reformatDate(String dateStr, String format) {
        try {
            // Assuming the input date format is "yyyy-MM-dd"
            SimpleDateFormat inputFormat = new SimpleDateFormat(DateUtils.DEFAULT_DATE_FORMAT);
            Date date = inputFormat.parse(dateStr);
            SimpleDateFormat outputFormat = new SimpleDateFormat(format);
            return outputFormat.format(date);
        } catch (ParseException e) {
            // Handle the exception, maybe log it and return the original dateStr
            return dateStr;
        }
    }
}
