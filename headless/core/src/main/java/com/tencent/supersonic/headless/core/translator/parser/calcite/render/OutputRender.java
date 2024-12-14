package com.tencent.supersonic.headless.core.translator.parser.calcite.render;

import com.tencent.supersonic.common.pojo.ColumnOrder;
import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.core.pojo.DataModel;
import com.tencent.supersonic.headless.core.pojo.OntologyQuery;
import com.tencent.supersonic.headless.core.translator.parser.calcite.S2CalciteSchema;
import com.tencent.supersonic.headless.core.translator.parser.calcite.node.MetricNode;
import com.tencent.supersonic.headless.core.translator.parser.calcite.node.SemanticNode;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.validate.SqlValidatorScope;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/** process the query result items from query request */
public class OutputRender extends Renderer {

    @Override
    public void render(OntologyQuery ontologyQuery, List<DataModel> dataModels,
            SqlValidatorScope scope, S2CalciteSchema schema, boolean nonAgg) throws Exception {
        EngineType engineType = schema.getOntology().getDatabase().getType();
        for (DimSchemaResp dimension : ontologyQuery.getDimensions()) {
            tableView.getMetric().add(SemanticNode.parse(dimension.getExpr(), scope, engineType));
        }
        for (MetricSchemaResp metric : ontologyQuery.getMetrics()) {
            if (MetricNode.isMetricField(metric.getName(), schema)) {
                // metric from field ignore
                continue;
            }
            tableView.getMetric().add(SemanticNode.parse(metric.getName(), scope, engineType));
        }

        if (ontologyQuery.getLimit() > 0) {
            SqlNode offset =
                    SemanticNode.parse(ontologyQuery.getLimit().toString(), scope, engineType);
            tableView.setOffset(offset);
        }
        if (!CollectionUtils.isEmpty(ontologyQuery.getOrder())) {
            List<SqlNode> orderList = new ArrayList<>();
            for (ColumnOrder columnOrder : ontologyQuery.getOrder()) {
                if (SqlStdOperatorTable.DESC.getName().equalsIgnoreCase(columnOrder.getOrder())) {
                    orderList.add(SqlStdOperatorTable.DESC.createCall(SqlParserPos.ZERO,
                            new SqlNode[] {SemanticNode.parse(columnOrder.getCol(), scope,
                                    engineType)}));
                } else {
                    orderList.add(SemanticNode.parse(columnOrder.getCol(), scope, engineType));
                }
            }
            tableView.setOrder(new SqlNodeList(orderList, SqlParserPos.ZERO));
        }
    }
}
