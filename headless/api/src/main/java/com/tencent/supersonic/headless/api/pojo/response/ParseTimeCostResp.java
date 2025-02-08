package com.tencent.supersonic.headless.api.pojo.response;

import lombok.Data;

import java.io.Serializable;

@Data
public class ParseTimeCostResp implements Serializable {

    private long parseStartTime;
    private long parseTime;
    private long sqlTime;

    public ParseTimeCostResp() {
        this.parseStartTime = System.currentTimeMillis();
    }
}
