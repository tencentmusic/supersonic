package com.tencent.supersonic.headless.core.translator.parser.calcite.node;

import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.headless.api.pojo.enums.MetricDefineType;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.core.translator.parser.calcite.S2CalciteSchema;
import lombok.Data;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.validate.SqlValidatorScope;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Data
public class MetricNode extends SemanticNode {

    private MetricSchemaResp metric;
    private Map<String, SqlNode> aggNode = new HashMap<>();
    private Map<String, SqlNode> nonAggNode = new HashMap<>();
    private Map<String, SqlNode> measureFilter = new HashMap<>();
    private Map<String, String> aggFunction = new HashMap<>();

    public static SqlNode build(MetricSchemaResp metric, SqlValidatorScope scope,
            EngineType engineType) throws Exception {
        return parse(metric.getExpr(), scope, engineType);
    }

    public static Boolean isMetricField(String name, S2CalciteSchema schema) {
        Optional<MetricSchemaResp> metric = schema.getMetrics().stream()
                .filter(m -> m.getName().equalsIgnoreCase(name)).findFirst();
        return metric.isPresent()
                && metric.get().getMetricDefineType().equals(MetricDefineType.FIELD);
    }

}
