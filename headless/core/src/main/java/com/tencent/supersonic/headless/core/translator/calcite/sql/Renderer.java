package com.tencent.supersonic.headless.core.translator.calcite.sql;


import com.tencent.supersonic.headless.api.pojo.enums.EngineType;
import com.tencent.supersonic.headless.core.pojo.MetricQueryParam;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Dimension;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Identify;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Measure;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Metric;
import com.tencent.supersonic.headless.core.translator.calcite.schema.SemanticSchema;
import com.tencent.supersonic.headless.core.translator.calcite.sql.node.MeasureNode;
import com.tencent.supersonic.headless.core.translator.calcite.sql.node.MetricNode;
import com.tencent.supersonic.headless.core.translator.calcite.sql.node.SemanticNode;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.DataSource;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Data;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.validate.SqlValidatorScope;

/**
 * process TableView
 */
@Data
public abstract class Renderer {

    protected TableView tableView = new TableView();

    public static Optional<Dimension> getDimensionByName(String name, DataSource datasource) {
        return datasource.getDimensions().stream().filter(d -> d.getName().equalsIgnoreCase(name)).findFirst();
    }

    public static Optional<Measure> getMeasureByName(String name, DataSource datasource) {
        return datasource.getMeasures().stream().filter(mm -> mm.getName().equalsIgnoreCase(name)).findFirst();
    }

    public static Optional<Metric> getMetricByName(String name, SemanticSchema schema) {
        Optional<Metric> metric = schema.getMetrics().stream().filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst();
        return metric;
    }

    public static Optional<Identify> getIdentifyByName(String name, DataSource datasource) {
        return datasource.getIdentifiers().stream().filter(i -> i.getName().equalsIgnoreCase(name)).findFirst();
    }

    public static MetricNode buildMetricNode(String metric, DataSource datasource, SqlValidatorScope scope,
                                             SemanticSchema schema, boolean nonAgg, String alias) throws Exception {
        Optional<Metric> metricOpt = getMetricByName(metric, schema);
        MetricNode metricNode = new MetricNode();
        EngineType engineType = EngineType.fromString(datasource.getType());
        if (metricOpt.isPresent()) {
            metricNode.setMetric(metricOpt.get());
            for (Measure m : metricOpt.get().getMetricTypeParams().getMeasures()) {
                Optional<Measure> measure = getMeasureByName(m.getName(), datasource);
                if (measure.isPresent()) {
                    metricNode.getNonAggNode()
                            .put(measure.get().getName(),
                                    MeasureNode.buildNonAgg(alias, measure.get(), scope, engineType));
                    metricNode.getAggNode()
                            .put(measure.get().getName(),
                                    MeasureNode.buildAgg(measure.get(), nonAgg, scope, engineType));
                    metricNode.getAggFunction().put(measure.get().getName(), measure.get().getAgg());

                } else {
                    metricNode.getNonAggNode().put(m.getName(), MeasureNode.buildNonAgg(alias, m, scope, engineType));
                    metricNode.getAggNode().put(m.getName(), MeasureNode.buildAgg(m, nonAgg, scope, engineType));
                    metricNode.getAggFunction().put(m.getName(), m.getAgg());
                }
                if (m.getConstraint() != null && !m.getConstraint().isEmpty()) {
                    metricNode.getMeasureFilter()
                            .put(m.getName(), SemanticNode.parse(m.getConstraint(), scope, engineType));
                }
            }
            return metricNode;
        }
        Optional<Measure> measure = getMeasureByName(metric, datasource);
        if (measure.isPresent()) {
            metricNode.getNonAggNode()
                    .put(measure.get().getName(), MeasureNode.buildNonAgg(alias, measure.get(), scope, engineType));
            metricNode.getAggNode()
                    .put(measure.get().getName(), MeasureNode.buildAgg(measure.get(), nonAgg, scope, engineType));
            metricNode.getAggFunction().put(measure.get().getName(), measure.get().getAgg());

            if (measure.get().getConstraint() != null && !measure.get().getConstraint().isEmpty()) {
                metricNode.getMeasureFilter().put(measure.get().getName(),
                        SemanticNode.parse(measure.get().getConstraint(), scope, engineType));
            }
        }
        return metricNode;
    }

    public static List<String> uniqList(List<String> list) {
        Set<String> tmp = new HashSet<>(list);
        return tmp.stream().collect(Collectors.toList());
    }

    public void setTable(SqlNode table) {
        tableView.setTable(table);
    }

    public SqlNode builder() {
        return tableView.build();
    }

    public SqlNode builderAs(String alias) throws Exception {
        return SemanticNode.buildAs(alias, tableView.build());
    }

    public abstract void render(MetricQueryParam metricCommand, List<DataSource> dataSources, SqlValidatorScope scope,
            SemanticSchema schema, boolean nonAgg) throws Exception;
}
