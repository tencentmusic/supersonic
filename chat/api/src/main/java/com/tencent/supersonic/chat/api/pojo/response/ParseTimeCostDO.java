package com.tencent.supersonic.chat.api.pojo.response;

import lombok.Data;

@Data
public class ParseTimeCostDO {
    private Long parseTime;
    private Long sqlTime;
}
