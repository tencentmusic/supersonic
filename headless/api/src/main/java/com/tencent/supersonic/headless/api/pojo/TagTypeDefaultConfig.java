package com.tencent.supersonic.headless.api.pojo;

import lombok.Data;

@Data
public class TagTypeDefaultConfig {

    private DefaultDisplayInfo defaultDisplayInfo;

    //default time to filter tag selection results
    private TimeDefaultConfig timeDefaultConfig = new TimeDefaultConfig();

}
