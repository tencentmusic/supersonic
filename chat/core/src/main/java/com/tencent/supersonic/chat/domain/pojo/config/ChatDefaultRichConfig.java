package com.tencent.supersonic.chat.domain.pojo.config;


import com.tencent.supersonic.common.constant.Constants;
import com.tencent.supersonic.common.pojo.SchemaItem;
import lombok.Data;

import java.util.List;

@Data
public class ChatDefaultRichConfig {

    private List<SchemaItem> dimensions;
    private List<SchemaItem> metrics;

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