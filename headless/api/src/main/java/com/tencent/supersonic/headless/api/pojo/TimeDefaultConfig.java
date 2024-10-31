package com.tencent.supersonic.headless.api.pojo;

import com.tencent.supersonic.common.pojo.enums.DatePeriodEnum;
import com.tencent.supersonic.common.pojo.enums.TimeMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimeDefaultConfig implements Serializable {

    /** default time span unit */
    private Integer unit = 1;

    private DatePeriodEnum period = DatePeriodEnum.DAY;

    private TimeMode timeMode = TimeMode.LAST;
}
