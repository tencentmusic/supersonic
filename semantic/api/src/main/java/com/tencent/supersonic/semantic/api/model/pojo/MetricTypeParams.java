package com.tencent.supersonic.semantic.api.model.pojo;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.Data;

@Data
public class MetricTypeParams {

    private List<Measure> measures = Lists.newArrayList();

    private String expr;

}
