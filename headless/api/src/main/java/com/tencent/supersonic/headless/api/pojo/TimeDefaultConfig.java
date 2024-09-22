package com.tencent.supersonic.headless.api.pojo;

import com.tencent.supersonic.common.pojo.enums.DatePeriodEnum;
import com.tencent.supersonic.common.pojo.enums.TimeMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimeDefaultConfig {

    /** default time span unit */
    private Integer unit = 1;

    private DatePeriodEnum period = DatePeriodEnum.DAY;

    private TimeMode timeMode = TimeMode.LAST;
}
