package com.tencent.supersonic.semantic.api.core.pojo;

import java.util.List;
import lombok.Data;

@Data
public class MetricTypeParams {

    private List<Measure> measures;

    private String expr;

}
