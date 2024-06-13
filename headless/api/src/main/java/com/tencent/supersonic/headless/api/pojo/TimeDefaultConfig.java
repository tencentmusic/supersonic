package com.tencent.supersonic.headless.api.pojo;


import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.TimeMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimeDefaultConfig {


    /**
     * default time span unit
     */
    private Integer unit = 1;

    /**
     * default time type: day
     * DAY, WEEK, MONTH, YEAR
     */
    private String period = Constants.DAY;

    private TimeMode timeMode = TimeMode.LAST;

}
