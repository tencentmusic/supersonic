package com.tencent.supersonic.semantic.api.model.response;

import lombok.Data;


@Data
public class MeasureResp {

    private String name;

    //sum max min avg count distinct
    private String agg;

    private String expr;

    private String constraint;

    private String alias;

    private Long datasourceId;

    private String datasourceName;

    private String datasourceBizName;

    private String bizName;

    private Long modelId;

}
