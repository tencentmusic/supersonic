package com.tencent.supersonic.headless.core.translator.parser.calcite.render;

import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.headless.core.pojo.OntologyQuery;
import com.tencent.supersonic.headless.core.translator.parser.calcite.S2CalciteSchema;
import com.tencent.supersonic.headless.core.translator.parser.calcite.TableView;
import com.tencent.supersonic.headless.core.translator.parser.calcite.node.FilterNode;
import com.tencent.supersonic.headless.core.translator.parser.calcite.node.MetricNode;
import com.tencent.supersonic.headless.core.translator.parser.calcite.node.SemanticNode;
import com.tencent.supersonic.headless.core.translator.parser.s2sql.Constants;
import com.tencent.supersonic.headless.core.translator.parser.s2sql.DataModel;
import com.tencent.supersonic.headless.core.translator.parser.s2sql.Metric;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.validate.SqlValidatorScope;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** process query specified filtering information */
public class FilterRender extends Renderer {

    @Override
    public void render(OntologyQuery metricCommand, List<DataModel> dataModels,
            SqlValidatorScope scope, S2CalciteSchema schema, boolean nonAgg) throws Exception {
        TableView tableView = super.tableView;
        SqlNode filterNode = null;
        List<String> queryMetrics = new ArrayList<>(metricCommand.getMetrics());
        List<String> queryDimensions = new ArrayList<>(metricCommand.getDimensions());
        EngineType engineType = schema.getOntology().getDatabaseType();

        if (metricCommand.getWhere() != null && !metricCommand.getWhere().isEmpty()) {
            filterNode = SemanticNode.parse(metricCommand.getWhere(), scope, engineType);
            Set<String> whereFields = new HashSet<>();
            FilterNode.getFilterField(filterNode, whereFields);
            List<String> fieldWhere = whereFields.stream().collect(Collectors.toList());
            Set<String> dimensions = new HashSet<>();
            Set<String> metrics = new HashSet<>();
            for (DataModel dataModel : dataModels) {
                SourceRender.whereDimMetric(fieldWhere, metricCommand.getMetrics(),
                        metricCommand.getDimensions(), dataModel, schema, dimensions, metrics);
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
                tableView.getMeasure()
                        .add(MetricNode.build(optionalMetric.get(), scope, engineType));
            } else {
                tableView.getMeasure().add(SemanticNode.parse(metric, scope, engineType));
            }
        }
        tableView.setMeasure(SemanticNode.deduplicateNode(tableView.getMeasure()));
        tableView.setDimension(SemanticNode.deduplicateNode(tableView.getDimension()));
        if (filterNode != null) {
            TableView filterView = new TableView();
            filterView.setTable(SemanticNode.buildAs(Constants.DATASOURCE_TABLE_FILTER_PREFIX,
                    tableView.build()));
            filterView.getFilter().add(filterNode);
            filterView.getMeasure().add(SqlIdentifier.star(SqlParserPos.ZERO));
            super.tableView = filterView;
        }
    }
}
