package com.tencent.supersonic.common.util;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.DatePeriodEnum;
import lombok.extern.slf4j.Slf4j;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Slf4j
public class DateUtils {

    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String FORMAT = "yyyyMMddHHmmss";
    private static final DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ofPattern(DATE_FORMAT);

    public static DateTimeFormatter getDateFormatter(String date, String[] formats) {
        for (int i = 0; i < formats.length; i++) {
            String format = formats[i];
            SimpleDateFormat dateFormat = new SimpleDateFormat(format);
            try {
                dateFormat.parse(date);
                return DateTimeFormatter.ofPattern(format);
            } catch (ParseException e) {
                log.warn("date parse has a exception:{}", e.toString());
            }
        }
        return DateTimeFormatter.ofPattern(formats[0]);
    }

    public static DateTimeFormatter getTimeFormatter(String date, String[] formats) {
        for (int i = 0; i < formats.length; i++) {
            String format = formats[i];
            try {
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(format);
                LocalDateTime.parse(date, dateTimeFormatter);
                return dateTimeFormatter;
            } catch (DateTimeParseException e) {
                log.warn("date parse has a exception:{}", e.toString());
            }
        }
        return DateTimeFormatter.ofPattern(formats[0]);
    }

    public static String getBeforeDate(int intervalDay) {
        return getBeforeDate(intervalDay, DatePeriodEnum.DAY);
    }

    public static String getBeforeDate(int intervalDay, DatePeriodEnum datePeriodEnum) {
        if (Objects.isNull(datePeriodEnum)) {
            datePeriodEnum = DatePeriodEnum.DAY;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        String currentDate = dateFormat.format(new Date());
        return getBeforeDate(currentDate, intervalDay, datePeriodEnum);
    }

    public static String getBeforeDate(String currentDate, DatePeriodEnum datePeriodEnum) {
        LocalDate specifiedDate = LocalDate.parse(currentDate, dateTimeFormatter);
        LocalDate startDate;
        switch (datePeriodEnum) {
            case MONTH:
                startDate = specifiedDate.withDayOfMonth(1);
                break;
            case YEAR:
                startDate = specifiedDate.withDayOfYear(1);
                break;
            default:
                startDate = specifiedDate;
        }

        return startDate.format(dateTimeFormatter);
    }

    public static String getBeforeDate(
            String currentDate, int intervalDay, DatePeriodEnum datePeriodEnum) {
        LocalDate specifiedDate = LocalDate.parse(currentDate, dateTimeFormatter);
        LocalDate result = null;
        switch (datePeriodEnum) {
            case DAY:
                result = specifiedDate.minusDays(intervalDay);
                break;
            case WEEK:
                result = specifiedDate.minusWeeks(intervalDay);
                if (intervalDay == 0) {
                    result =
                            result.with(
                                    TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                }
                break;
            case MONTH:
                result = specifiedDate.minusMonths(intervalDay);
                if (intervalDay == 0) {
                    result = result.with(TemporalAdjusters.firstDayOfMonth());
                }
                break;
            case QUARTER:
                result = specifiedDate.minusMonths(intervalDay * 3L);
                if (intervalDay == 0) {
                    TemporalAdjuster firstDayOfQuarter =
                            temporal -> {
                                LocalDate tempDate = LocalDate.from(temporal);
                                int month = tempDate.get(ChronoField.MONTH_OF_YEAR);
                                int firstMonthOfQuarter = ((month - 1) / 3) * 3 + 1;
                                return tempDate.with(ChronoField.MONTH_OF_YEAR, firstMonthOfQuarter)
                                        .with(TemporalAdjusters.firstDayOfMonth());
                            };
                    result = result.with(firstDayOfQuarter);
                }
                break;
            case YEAR:
                result = specifiedDate.minusYears(intervalDay);
                if (intervalDay == 0) {
                    result = result.with(TemporalAdjusters.firstDayOfYear());
                }
                break;
            default:
        }
        if (Objects.nonNull(result)) {
            return result.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        }

        return null;
    }

    public static String format(Date date) {
        DateFormat dateFormat;
        if (containsTime(date)) {
            dateFormat = new SimpleDateFormat(DateUtils.TIME_FORMAT);
        } else {
            dateFormat = new SimpleDateFormat(DateUtils.DATE_FORMAT);
        }
        return dateFormat.format(date);
    }

    public static String format(Date date, String format) {
        DateFormat dateFormat = new SimpleDateFormat(format);
        return dateFormat.format(date);
    }

    private static boolean containsTime(Date date) {
        DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        String timeString = timeFormat.format(date);
        return !timeString.equals("00:00:00");
    }

    public static List<String> getDateList(String startDateStr, String endDateStr, String period) {
        try {
            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate endDate = LocalDate.parse(endDateStr);
            List<String> datesInRange = new ArrayList<>();
            LocalDate currentDate = startDate;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
            while (!currentDate.isAfter(endDate)) {
                if (Constants.MONTH.equals(period)) {
                    datesInRange.add(currentDate.format(formatter));
                    currentDate = currentDate.plusMonths(1);
                } else if (Constants.WEEK.equals(period)) {
                    datesInRange.add(currentDate.format(DateTimeFormatter.ISO_DATE));
                    currentDate = currentDate.plusWeeks(1);
                } else {
                    datesInRange.add(currentDate.format(DateTimeFormatter.ISO_DATE));
                    currentDate = currentDate.plusDays(1);
                }
            }
            return datesInRange;
        } catch (Exception e) {
            log.info("parse date failed, startDate:{}, endDate:{}", startDateStr, endDateStr, e);
        }
        return Lists.newArrayList();
    }

    public static boolean isAnyDateString(String value) {
        List<String> formats = Arrays.asList("yyyy-MM-dd", "yyyy-MM", "yyyy/MM/dd");
        return isAnyDateString(value, formats);
    }

    public static boolean isAnyDateString(String value, List<String> formats) {
        for (String format : formats) {
            if (isDateString(value, format)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isDateString(String value, String format) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            LocalDate.parse(value, formatter);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
