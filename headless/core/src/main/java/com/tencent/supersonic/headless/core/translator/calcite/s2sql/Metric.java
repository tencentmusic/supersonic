package com.tencent.supersonic.headless.core.translator.calcite.s2sql;

import com.tencent.supersonic.headless.core.translator.calcite.schema.SemanticItem;
import lombok.Data;

import java.util.List;

@Data
public class Metric implements SemanticItem {

    private String name;
    private List<String> owners;
    private String type;
    private MetricTypeParams metricTypeParams;

    @Override
    public String getName() {
        return name;
    }
}
