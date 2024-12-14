package com.tencent.supersonic.headless.core.translator.parser.calcite.render;

import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.headless.api.pojo.Identify;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.core.pojo.DataModel;
import com.tencent.supersonic.headless.core.pojo.OntologyQuery;
import com.tencent.supersonic.headless.core.translator.parser.calcite.S2CalciteSchema;
import com.tencent.supersonic.headless.core.translator.parser.calcite.TableView;
import com.tencent.supersonic.headless.core.translator.parser.calcite.node.DataModelNode;
import com.tencent.supersonic.headless.core.translator.parser.calcite.node.SemanticNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.validate.SqlValidatorScope;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** process the table dataSet from the defined data model schema */
@Slf4j
public class SourceRender extends Renderer {

    public static TableView renderOne(Set<MetricSchemaResp> queryMetrics,
            Set<DimSchemaResp> queryDimensions, DataModel dataModel, SqlValidatorScope scope,
            S2CalciteSchema schema) {
        TableView tableView = new TableView();
        EngineType engineType = schema.getOntology().getDatabase().getType();
        Set<String> queryFields = tableView.getFields();
        queryMetrics.stream().forEach(m -> queryFields.addAll(m.getFields()));
        queryDimensions.stream().forEach(m -> queryFields.add(m.getBizName()));

        try {
            for (String field : queryFields) {
                tableView.getSelect().add(SemanticNode.parse(field, scope, engineType));
            }
            tableView.setTable(DataModelNode.build(dataModel, scope));
        } catch (Exception e) {
            log.error("Failed to create sqlNode for data model {}", dataModel);
        }

        return tableView;
    }

    public static boolean isDimension(String name, DataModel dataModel, S2CalciteSchema schema) {
        Optional<DimSchemaResp> dimension = dataModel.getDimensions().stream()
                .filter(d -> d.getName().equalsIgnoreCase(name)).findFirst();
        if (dimension.isPresent()) {
            return true;
        }
        Optional<Identify> identify = dataModel.getIdentifiers().stream()
                .filter(i -> i.getName().equalsIgnoreCase(name)).findFirst();
        if (identify.isPresent()) {
            return true;
        }
        if (schema.getDimensions().containsKey(dataModel.getName())) {
            Optional<DimSchemaResp> dataSourceDim = schema.getDimensions().get(dataModel.getName())
                    .stream().filter(d -> d.getName().equalsIgnoreCase(name)).findFirst();
            if (dataSourceDim.isPresent()) {
                return true;
            }
        }
        return false;
    }


    public void render(OntologyQuery ontologyQuery, List<DataModel> dataModels,
            SqlValidatorScope scope, S2CalciteSchema schema, boolean nonAgg) throws Exception {
        if (dataModels.size() == 1) {
            DataModel dataModel = dataModels.get(0);
            tableView = renderOne(ontologyQuery.getMetrics(), ontologyQuery.getDimensions(),
                    dataModel, scope, schema);
        } else {
            JoinRender joinRender = new JoinRender();
            joinRender.render(ontologyQuery, dataModels, scope, schema, nonAgg);
            tableView = joinRender.getTableView();
        }
    }

}
