package com.tencent.supersonic.chat.config;


import com.tencent.supersonic.common.pojo.Constants;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChatDefaultConfig {

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

}