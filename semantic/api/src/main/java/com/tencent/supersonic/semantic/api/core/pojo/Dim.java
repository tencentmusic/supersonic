package com.tencent.supersonic.semantic.api.core.pojo;

import com.tencent.supersonic.common.constant.Constants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Dim {

    private String name;

    private String type;

    private String expr;

    private String dateFormat = Constants.DAY_FORMAT;

    private DimensionTimeTypeParams typeParams;

    private Integer isCreateDimension = 0;

    private String bizName;

    public static Dim getDefault() {
        return new Dim("日期", "time", "2023-05-28",
                Constants.DAY_FORMAT,
                new DimensionTimeTypeParams("true", "day"),
                0, "imp_date"
        );
    }

}
