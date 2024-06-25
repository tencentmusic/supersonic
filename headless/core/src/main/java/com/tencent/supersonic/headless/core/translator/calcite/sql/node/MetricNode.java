package com.tencent.supersonic.headless.core.translator.calcite.sql.node;


import com.tencent.supersonic.headless.api.pojo.enums.EngineType;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Metric;
import com.tencent.supersonic.headless.core.translator.calcite.schema.SemanticSchema;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import lombok.Data;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.validate.SqlValidatorScope;

@Data
public class MetricNode extends SemanticNode {

    private Metric metric;
    private Map<String, SqlNode> aggNode = new HashMap<>();
    private Map<String, SqlNode> nonAggNode = new HashMap<>();
    private Map<String, SqlNode> measureFilter = new HashMap<>();
    private Map<String, String> aggFunction = new HashMap<>();

    public static SqlNode build(Metric metric, SqlValidatorScope scope, EngineType engineType) throws Exception {
        if (metric.getMetricTypeParams() == null || metric.getMetricTypeParams().getExpr() == null
                || metric.getMetricTypeParams().getExpr().isEmpty()) {
            return parse(metric.getName(), scope, engineType);
        }
        SqlNode sqlNode = parse(metric.getMetricTypeParams().getExpr(), scope, engineType);
        return buildAs(metric.getName(), sqlNode);
    }

    public static Boolean isMetricField(String name, SemanticSchema schema) {
        Optional<Metric> metric = schema.getMetrics().stream().filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst();
        return metric.isPresent() && metric.get().getMetricTypeParams().isFieldMetric();
    }

    public static Boolean isMetricField(Metric metric) {
        return metric.getMetricTypeParams().isFieldMetric();
    }

}
