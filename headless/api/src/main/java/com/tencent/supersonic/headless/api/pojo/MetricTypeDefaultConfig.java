package com.tencent.supersonic.headless.api.pojo;

import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.TimeMode;
import lombok.Data;

@Data
public class MetricTypeDefaultConfig {

    private TimeDefaultConfig timeDefaultConfig =
            new TimeDefaultConfig(7, Constants.DAY, TimeMode.RECENT);

}
