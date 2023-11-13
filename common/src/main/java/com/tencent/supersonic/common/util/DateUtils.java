package com.tencent.supersonic.common.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import com.tencent.supersonic.common.pojo.Constants;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DateUtils {

    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String FORMAT = "yyyyMMddHHmmss";

    public static Integer currentYear() {
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        String time = dateFormat.format(date).replaceAll("-", "");
        int year = Integer.parseInt(time.substring(0, 4));
        return year;
    }

    public static DateTimeFormatter getDateFormatter(String date, String[] formats) {
        for (int i = 0; i < formats.length; i++) {
            String format = formats[i];
            SimpleDateFormat dateFormat = new SimpleDateFormat(format);
            try {
                dateFormat.parse(date);
                return DateTimeFormatter.ofPattern(format);
            } catch (Exception e) {
                log.info("date parse has a exception:{}", e.toString());
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
            } catch (Exception e) {
                log.info("date parse has a exception:{}", e.toString());
            }
        }
        return DateTimeFormatter.ofPattern(formats[0]);
    }


    public static String getBeforeDate(int intervalDay) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DAY_OF_MONTH, -intervalDay);
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        return dateFormat.format(calendar.getTime());
    }


    public static String getBeforeDate(int intervalDay, DatePeriodEnum datePeriodEnum) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        String currentDate = dateFormat.format(new Date());
        return getBeforeDate(currentDate, intervalDay, datePeriodEnum);
    }

    public static String getBeforeDate(String date, int intervalDay, DatePeriodEnum datePeriodEnum) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
        LocalDate currentDate = LocalDate.parse(date, dateTimeFormatter);
        LocalDate result = null;
        switch (datePeriodEnum) {
            case DAY:
                result = currentDate.minusDays(intervalDay);
                break;
            case WEEK:
                result = currentDate.minusWeeks(intervalDay);
                if (intervalDay == 0) {
                    result = result.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                }
                break;
            case MONTH:
                result = currentDate.minusMonths(intervalDay);
                if (intervalDay == 0) {
                    result = result.with(TemporalAdjusters.firstDayOfMonth());
                }
                break;
            case YEAR:
                result = currentDate.minusYears(intervalDay);
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
        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);

        List<String> datesInRange = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            datesInRange.add(currentDate.format(DateTimeFormatter.ISO_DATE));
            if (Constants.MONTH.equals(period)) {
                currentDate = currentDate.plusMonths(1);
            } else if (Constants.WEEK.equals(period)) {
                currentDate = currentDate.plusWeeks(1);
            } else {
                currentDate = currentDate.plusDays(1);
            }
        }
        return datesInRange;
    }

}
