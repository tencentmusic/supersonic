package com.tencent.supersonic.chat.api.pojo.response;

import lombok.Data;

@Data
public class ParseTimeCostDO {

    private long parseStartTime;
    private long parseTime;
    private long sqlTime;

    public ParseTimeCostDO() {
        this.parseStartTime = System.currentTimeMillis();
    }
}
