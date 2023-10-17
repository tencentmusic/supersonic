package com.tencent.supersonic.semantic.api.model.pojo;

import com.tencent.supersonic.common.pojo.Constants;
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

    private String description;

    public Dim(String name, String bizName, String type, Integer isCreateDimension) {
        this.name = name;
        this.type = type;
        this.isCreateDimension = isCreateDimension;
        this.bizName = bizName;
    }

    public Dim(String name, String type, String expr, String dateFormat, DimensionTimeTypeParams typeParams,
               Integer isCreateDimension, String bizName) {
        this.name = name;
        this.type = type;
        this.expr = expr;
        this.dateFormat = dateFormat;
        this.typeParams = typeParams;
        this.isCreateDimension = isCreateDimension;
        this.bizName = bizName;
    }

    public static Dim getDefault() {
        return new Dim("日期", "time", "2023-05-28",
                Constants.DAY_FORMAT,
                new DimensionTimeTypeParams("true", "day"),
                0, "imp_date"
        );
    }

}
