package com.tencent.supersonic.chat.api.pojo.request;


import com.tencent.supersonic.common.pojo.Constants;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChatDefaultConfigReq {

    private List<Long> dimensionIds = new ArrayList<>();
    private List<Long> metricIds = new ArrayList<>();

    /**
     * default time span unit
     */
    private Integer unit = 1;

    /**
     * default time type: day
     * DAY, WEEK, MONTH, YEAR
     */
    private String period = Constants.DAY;

    private TimeMode timeMode = TimeMode.LAST;

    public enum TimeMode {
        /**
         * date mode
         * LAST   - a certain time
         * RECENT - a period time
         */
        LAST, RECENT
    }

}