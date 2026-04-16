package com.tencent.supersonic.headless.api.pojo.schema;

import lombok.Data;

import java.util.List;

@Data
public class MetricSchema {

    private String name;

    private List<String> owners;

    private String type;

    private MetricTypeParamsSchema typeParams;
}
