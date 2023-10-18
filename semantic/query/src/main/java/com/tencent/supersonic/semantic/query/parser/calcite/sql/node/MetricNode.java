package com.tencent.supersonic.semantic.query.parser.calcite.sql.node;


import com.tencent.supersonic.semantic.query.parser.calcite.s2ql.Metric;
import java.util.HashMap;
import java.util.Map;
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

    public static SqlNode build(Metric metric, SqlValidatorScope scope) throws Exception {
        if (metric.getMetricTypeParams() == null || metric.getMetricTypeParams().getExpr() == null
                || metric.getMetricTypeParams().getExpr().isEmpty()) {
            return parse(metric.getName(), scope);
        }
        SqlNode sqlNode = parse(metric.getMetricTypeParams().getExpr(), scope);
        return buildAs(metric.getName(), sqlNode);
    }

}
