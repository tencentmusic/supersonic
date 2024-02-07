package com.tencent.supersonic.headless.core.parser.calcite.sql.render;


import com.tencent.supersonic.headless.api.pojo.enums.EngineType;
import com.tencent.supersonic.headless.core.pojo.MetricQueryParam;
import com.tencent.supersonic.common.pojo.ColumnOrder;
import com.tencent.supersonic.headless.core.parser.calcite.s2sql.DataSource;
import com.tencent.supersonic.headless.core.parser.calcite.schema.SemanticSchema;
import com.tencent.supersonic.headless.core.parser.calcite.sql.Renderer;
import com.tencent.supersonic.headless.core.parser.calcite.sql.TableView;
import com.tencent.supersonic.headless.core.parser.calcite.sql.node.MetricNode;
import com.tencent.supersonic.headless.core.parser.calcite.sql.node.SemanticNode;
import java.util.ArrayList;
import java.util.List;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.validate.SqlValidatorScope;
import org.springframework.util.CollectionUtils;

/**
 * process the query result items from query request
 */
public class OutputRender extends Renderer {

    @Override
    public void render(MetricQueryParam metricCommand, List<DataSource> dataSources, SqlValidatorScope scope,
            SemanticSchema schema, boolean nonAgg) throws Exception {
        TableView selectDataSet = super.tableView;
        EngineType engineType = EngineType.fromString(schema.getSemanticModel().getDatabase().getType());
        for (String dimension : metricCommand.getDimensions()) {
            selectDataSet.getMeasure().add(SemanticNode.parse(dimension, scope, engineType));
        }
        for (String metric : metricCommand.getMetrics()) {
            if (MetricNode.isMetricField(metric, schema)) {
                // metric from field ignore
                continue;
            }
            selectDataSet.getMeasure().add(SemanticNode.parse(metric, scope, engineType));
        }

        if (metricCommand.getLimit() > 0) {
            SqlNode offset = SemanticNode.parse(metricCommand.getLimit().toString(), scope, engineType);
            selectDataSet.setOffset(offset);
        }
        if (!CollectionUtils.isEmpty(metricCommand.getOrder())) {
            List<SqlNode> orderList = new ArrayList<>();
            for (ColumnOrder columnOrder : metricCommand.getOrder()) {
                if (SqlStdOperatorTable.DESC.getName().equalsIgnoreCase(columnOrder.getOrder())) {
                    orderList.add(SqlStdOperatorTable.DESC.createCall(SqlParserPos.ZERO,
                            new SqlNode[]{SemanticNode.parse(columnOrder.getCol(), scope, engineType)}));
                } else {
                    orderList.add(SemanticNode.parse(columnOrder.getCol(), scope, engineType));
                }
            }
            selectDataSet.setOrder(new SqlNodeList(orderList, SqlParserPos.ZERO));
        }
    }
}
