package com.tencent.supersonic.common.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DateUtils {

    public static final String DATE_FORMAT_DOT = "yyyy-MM-dd";

    public static Integer currentYear() {
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_DOT);
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
            }
        }
        return DateTimeFormatter.ofPattern(formats[0]);
    }


    public static String getBeforeDate(int intervalDay) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DAY_OF_MONTH, -intervalDay);
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_DOT);
        return dateFormat.format(calendar.getTime());
    }


    public static String getBeforeDate(String date, int intervalDay, DatePeriodEnum datePeriodEnum) {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_DOT);
        try {
            calendar.setTime(dateFormat.parse(date));
        } catch (ParseException e) {
            log.error("parse error");
        }
        switch (datePeriodEnum) {
            case DAY:
                calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - intervalDay);
                break;
            case WEEK:
                calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - intervalDay * 7);
                break;
            case MONTH:
                calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - intervalDay);
                break;
            case YEAR:
                calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - intervalDay);
                break;
            default:
        }
        return dateFormat.format(calendar.getTime());
    }
}
