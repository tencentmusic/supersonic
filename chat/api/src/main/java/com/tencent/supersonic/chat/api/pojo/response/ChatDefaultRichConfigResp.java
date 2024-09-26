package com.tencent.supersonic.chat.api.pojo.response;

import com.tencent.supersonic.common.pojo.enums.DatePeriodEnum;
import com.tencent.supersonic.common.pojo.enums.TimeMode;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import lombok.Data;

import java.util.List;

@Data
public class ChatDefaultRichConfigResp {

    private List<SchemaElement> dimensions;
    private List<SchemaElement> metrics;

    /** default time span unit */
    private Integer unit = 1;

    /** default time type: DAY, WEEK, MONTH, YEAR */
    private DatePeriodEnum period = DatePeriodEnum.DAY;

    private TimeMode timeMode;
}
