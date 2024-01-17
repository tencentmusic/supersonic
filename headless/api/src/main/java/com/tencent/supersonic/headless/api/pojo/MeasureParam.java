package com.tencent.supersonic.headless.api.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MeasureParam {

    private String bizName;

    private String constraint;

    private String agg;

    public MeasureParam(String bizName, String constraint) {
        this.bizName = bizName;
        this.constraint = constraint;
    }

}
