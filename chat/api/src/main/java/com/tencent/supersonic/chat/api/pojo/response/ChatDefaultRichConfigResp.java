package com.tencent.supersonic.chat.api.pojo.response;


import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.TimeMode;
import lombok.Data;

import java.util.List;

@Data
public class ChatDefaultRichConfigResp {

    private List<SchemaElement> dimensions;
    private List<SchemaElement> metrics;


    /**
     * default time span unit
     */
    private Integer unit = 1;

    /**
     * default time type:
     * DAY, WEEK, MONTH, YEAR
     */
    private String period = Constants.DAY;

    private TimeMode timeMode;

}