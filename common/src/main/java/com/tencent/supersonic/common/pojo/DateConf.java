package com.tencent.supersonic.common.pojo;

import static java.time.LocalDate.now;

import com.tencent.supersonic.common.constant.Constants;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class DateConf {

    private static final long serialVersionUID = 3074129990945004340L;


    private DateMode dateMode;

    /**
     * like 2021-10-22, dateMode=1
     */
    private String startDate = now().plusDays(-1).toString();
    private String endDate = now().toString();

    /**
     * [2021-10-22, 2022-01-22], dateMode=2
     */
    private List<String> dateList = new ArrayList<>();

    /**
     * the last unit time unit,
     * such as the last 7 days, unit = 7
     */
    private Integer unit = 1;

    /**
     * DAY,WEEK,MONTH
     */
    private String period = Constants.DAY;

    /**
     * the text parse from , example "last 7 days" , "last mouth"
     */
    private String text;

    public enum DateMode {
        /**
         * date mode
         * 1 - between, continuous value,
         * 2 - list discrete value,
         * 3 - recent time units,
         * 4 - advance time until data is available
         */
        BETWEEN_CONTINUOUS, LIST_DISCRETE, RECENT_UNITS, AVAILABLE_TIME
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"dateMode\":")
                .append(dateMode);
        sb.append(",\"startDate\":\"")
                .append(startDate).append('\"');
        sb.append(",\"endDate\":\"")
                .append(endDate).append('\"');
        sb.append(",\"dateList\":")
                .append(dateList);
        sb.append(",\"unit\":")
                .append(unit);
        sb.append(",\"period\":\"")
                .append(period).append('\"');
        sb.append(",\"text\":\"")
                .append(text).append('\"');
        sb.append('}');
        return sb.toString();
    }
}