package com.tencent.supersonic.headless.api.pojo;

import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.headless.api.pojo.enums.DimensionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Dimension {

    private String name;

    private DimensionType type;

    private String expr;

    private String dateFormat = Constants.DAY_FORMAT;

    private DimensionTimeTypeParams typeParams;

    private Integer isCreateDimension = 0;

    private String bizName;

    private String description;

    public Dimension(String name, String bizName, DimensionType type, Integer isCreateDimension) {
        this.name = name;
        this.type = type;
        this.isCreateDimension = isCreateDimension;
        this.bizName = bizName;
        this.expr = bizName;
    }

    public Dimension(String name, String bizName, String expr, DimensionType type,
            Integer isCreateDimension) {
        this.name = name;
        this.type = type;
        this.isCreateDimension = isCreateDimension;
        this.bizName = bizName;
        this.expr = expr;
    }

    public Dimension(String name, String bizName, DimensionType type, Integer isCreateDimension,
            String expr, String dateFormat, DimensionTimeTypeParams typeParams) {
        this.name = name;
        this.type = type;
        this.expr = expr;
        this.dateFormat = dateFormat;
        this.typeParams = typeParams;
        this.isCreateDimension = isCreateDimension;
        this.bizName = bizName;
    }

    public static Dimension getDefault() {
        return new Dimension("数据日期", "imp_date", DimensionType.partition_time, 0, "imp_date",
                Constants.DAY_FORMAT, new DimensionTimeTypeParams("false", "day"));
    }

    public String getFieldName() {
        return bizName;
    }
}
