package com.tencent.supersonic.common.util;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateUtils {

    public static Integer currentYear(){
        Date date = new Date();
        SimpleDateFormat dateFormat= new SimpleDateFormat("yyyy-MM-dd");
        String time=dateFormat.format(date).replaceAll("-","");
        int year = Integer.parseInt(time.substring(0,4));
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
                LocalDateTime.parse(date,dateTimeFormatter);
                return dateTimeFormatter;
            } catch (Exception e) {
            }
        }
        return DateTimeFormatter.ofPattern(formats[0]);
    }
}
