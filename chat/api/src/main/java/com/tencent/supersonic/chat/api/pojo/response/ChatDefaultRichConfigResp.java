package com.tencent.supersonic.chat.api.pojo.response;


import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.request.ChatDefaultConfigReq;
import com.tencent.supersonic.common.pojo.Constants;
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
     * default time type: day
     * DAY, WEEK, MONTH, YEAR
     */
    private String period = Constants.DAY;

    private ChatDefaultConfigReq.TimeMode timeMode;

}