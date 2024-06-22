package com.tencent.supersonic.headless.core.translator.calcite.sql.render;


import com.tencent.supersonic.headless.api.pojo.enums.EngineType;
import com.tencent.supersonic.headless.core.pojo.MetricQueryParam;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Metric;
import com.tencent.supersonic.headless.core.translator.calcite.schema.SemanticSchema;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Constants;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.DataSource;
import com.tencent.supersonic.headless.core.translator.calcite.sql.Renderer;
import com.tencent.supersonic.headless.core.translator.calcite.sql.TableView;
import com.tencent.supersonic.headless.core.translator.calcite.sql.node.FilterNode;
import com.tencent.supersonic.headless.core.translator.calcite.sql.node.MetricNode;
import com.tencent.supersonic.headless.core.translator.calcite.sql.node.SemanticNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.validate.SqlValidatorScope;

/**
 * process query specified filtering information
 */
public class FilterRender extends Renderer {

    @Override
    public void render(MetricQueryParam metricCommand, List<DataSource> dataSources, SqlValidatorScope scope,
                       SemanticSchema schema, boolean nonAgg) throws Exception {
        TableView tableView = super.tableView;
        SqlNode filterNode = null;
        List<String> queryMetrics = new ArrayList<>(metricCommand.getMetrics());
        List<String> queryDimensions = new ArrayList<>(metricCommand.getDimensions());
        EngineType engineType = EngineType.fromString(schema.getSemanticModel().getDatabase().getType());

        if (metricCommand.getWhere() != null && !metricCommand.getWhere().isEmpty()) {
            filterNode = SemanticNode.parse(metricCommand.getWhere(), scope, engineType);
            Set<String> whereFields = new HashSet<>();
            FilterNode.getFilterField(filterNode, whereFields);
            List<String> fieldWhere = whereFields.stream().collect(Collectors.toList());
            Set<String> dimensions = new HashSet<>();
            Set<String> metrics = new HashSet<>();
            for (DataSource dataSource : dataSources) {
                SourceRender.whereDimMetric(fieldWhere, metricCommand.getMetrics(), metricCommand.getDimensions(),
                        dataSource, schema, dimensions, metrics);
            }
            queryMetrics.addAll(metrics);
            queryDimensions.addAll(dimensions);
        }
        for (String dimension : queryDimensions) {
            tableView.getMeasure().add(SemanticNode.parse(dimension, scope, engineType));
        }
        for (String metric : queryMetrics) {
            Optional<Metric> optionalMetric = Renderer.getMetricByName(metric, schema);
            if (optionalMetric.isPresent() && MetricNode.isMetricField(optionalMetric.get())) {
                // metric from field ignore
                continue;
            }
            if (optionalMetric.isPresent()) {
                tableView.getMeasure().add(MetricNode.build(optionalMetric.get(), scope, engineType));
            } else {
                tableView.getMeasure().add(SemanticNode.parse(metric, scope, engineType));
            }
        }
        if (filterNode != null) {
            TableView filterView = new TableView();
            filterView.setTable(SemanticNode.buildAs(Constants.DATASOURCE_TABLE_FILTER_PREFIX, tableView.build()));
            filterView.getFilter().add(filterNode);
            filterView.getMeasure().add(SqlIdentifier.star(SqlParserPos.ZERO));
            super.tableView = filterView;
        }
    }
}
