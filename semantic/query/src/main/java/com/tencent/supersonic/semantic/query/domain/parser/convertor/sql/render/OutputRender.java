package com.tencent.supersonic.semantic.query.domain.parser.convertor.sql.render;


import com.tencent.supersonic.semantic.api.query.request.MetricReq;
import com.tencent.supersonic.common.pojo.ColumnOrder;
import com.tencent.supersonic.semantic.query.domain.parser.convertor.sql.Renderer;
import com.tencent.supersonic.semantic.query.domain.parser.convertor.sql.TableView;
import com.tencent.supersonic.semantic.query.domain.parser.convertor.sql.node.SemanticNode;
import com.tencent.supersonic.semantic.query.domain.parser.dsl.DataSource;
import com.tencent.supersonic.semantic.query.domain.parser.schema.SemanticSchema;
import java.util.ArrayList;
import java.util.List;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.validate.SqlValidatorScope;
import org.springframework.util.CollectionUtils;

public class OutputRender extends Renderer {

    @Override
    public void render(MetricReq metricCommand, List<DataSource> dataSources, SqlValidatorScope scope,
            SemanticSchema schema, boolean nonAgg) throws Exception {
        TableView selectDataSet = super.tableView;
        for (String dimension : metricCommand.getDimensions()) {
            selectDataSet.getMeasure().add(SemanticNode.parse(dimension, scope));
        }
        for (String metric : metricCommand.getMetrics()) {
            selectDataSet.getMeasure().add(SemanticNode.parse(metric, scope));
        }

        if (metricCommand.getLimit() > 0) {
            SqlNode offset = SemanticNode.parse(metricCommand.getLimit().toString(), scope);
            selectDataSet.setOffset(offset);
        }
        if (!CollectionUtils.isEmpty(metricCommand.getOrder())) {
            List<SqlNode> orderList = new ArrayList<>();
            for (ColumnOrder columnOrder : metricCommand.getOrder()) {
                if (SqlStdOperatorTable.DESC.getName().equalsIgnoreCase(columnOrder.getOrder())) {
                    orderList.add(SqlStdOperatorTable.DESC.createCall(SqlParserPos.ZERO,
                            new SqlNode[]{SemanticNode.parse(columnOrder.getCol(), scope)}));
                } else {
                    orderList.add(SemanticNode.parse(columnOrder.getCol(), scope));
                }
            }
            selectDataSet.setOrder(new SqlNodeList(orderList, SqlParserPos.ZERO));
        }
    }
}
