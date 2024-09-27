package com.tencent.supersonic.headless.api.pojo;

import com.tencent.supersonic.common.pojo.enums.DatePeriodEnum;
import com.tencent.supersonic.common.pojo.enums.TimeMode;
import lombok.Data;

@Data
public class AggregateTypeDefaultConfig {

    private TimeDefaultConfig timeDefaultConfig =
            new TimeDefaultConfig(7, DatePeriodEnum.DAY, TimeMode.RECENT);

    private Long limit;
}
