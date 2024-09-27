package com.tencent.supersonic.headless.api.pojo;

import com.tencent.supersonic.common.pojo.Constants;
import lombok.Data;

@Data
public class DetailTypeDefaultConfig {

    private DefaultDisplayInfo defaultDisplayInfo;

    // default time to filter tag selection results
    private TimeDefaultConfig timeDefaultConfig = new TimeDefaultConfig();

    private long limit = Constants.DEFAULT_DETAIL_LIMIT;
}
