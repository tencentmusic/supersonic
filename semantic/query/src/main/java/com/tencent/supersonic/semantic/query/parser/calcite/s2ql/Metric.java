package com.tencent.supersonic.semantic.query.parser.calcite.s2ql;


import com.tencent.supersonic.semantic.query.parser.calcite.schema.SemanticItem;
import java.util.List;
import lombok.Data;


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
