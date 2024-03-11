package com.tencent.supersonic.headless.api.pojo.response;

import lombok.Data;

@Data
public class ParseTimeCostResp {

    private long parseStartTime;
    private long parseTime;
    private long sqlTime;

    public ParseTimeCostResp() {
        this.parseStartTime = System.currentTimeMillis();
    }
}
