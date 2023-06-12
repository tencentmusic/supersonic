package com.tencent.supersonic.semantic.query.domain.parser.dsl;


import com.tencent.supersonic.semantic.query.domain.parser.schema.SemanticItem;
import java.util.List;
import lombok.Data;


@Data
public class Metric implements SemanticItem {

    private String name;

    @Override
    public String getName() {
        return name;
    }


    private List<String> owners;

    private String type;

    private MetricTypeParams metricTypeParams;
}
