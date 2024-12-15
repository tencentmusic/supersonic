package com.tencent.supersonic.headless.core.translator.parser.calcite.render;

import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.headless.api.pojo.Identify;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.core.pojo.DataModel;
import com.tencent.supersonic.headless.core.pojo.JoinRelation;
import com.tencent.supersonic.headless.core.pojo.OntologyQuery;
import com.tencent.supersonic.headless.core.translator.parser.calcite.S2CalciteSchema;
import com.tencent.supersonic.headless.core.translator.parser.calcite.TableView;
import com.tencent.supersonic.headless.core.translator.parser.calcite.node.DataModelNode;
import com.tencent.supersonic.headless.core.translator.parser.calcite.node.IdentifyNode;
import com.tencent.supersonic.headless.core.translator.parser.calcite.node.SemanticNode;
import com.tencent.supersonic.headless.core.translator.parser.s2sql.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.validate.SqlValidatorScope;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/** process the join conditions when the source number is greater than 1 */
@Slf4j
public class JoinRender extends Renderer {

    @Override
    public void render(OntologyQuery ontologyQuery, List<DataModel> dataModels,
            SqlValidatorScope scope, S2CalciteSchema schema) throws Exception {
        SqlNode left = null;
        TableView leftTable = null;
        Map<String, SqlNode> outerSelect = new HashMap<>();
        Map<String, String> beforeModels = new HashMap<>();
        EngineType engineType = schema.getOntology().getDatabase().getType();

        for (int i = 0; i < dataModels.size(); i++) {
            final DataModel dataModel = dataModels.get(i);
            final Set<DimSchemaResp> queryDimensions =
                    ontologyQuery.getDimensionsByModel(dataModel.getId());
            final Set<MetricSchemaResp> queryMetrics =
                    ontologyQuery.getMetricsByModel(dataModel.getId());

            List<String> primary = new ArrayList<>();
            for (Identify identify : dataModel.getIdentifiers()) {
                primary.add(identify.getName());
            }

            TableView tableView =
                    renderOne(queryMetrics, queryDimensions, dataModel, scope, schema);
            log.info("tableView {}", StringUtils.normalizeSpace(tableView.getTable().toString()));
            String alias = Constants.JOIN_TABLE_PREFIX + dataModel.getName();
            tableView.setAlias(alias);
            tableView.setPrimary(primary);
            tableView.setDataModel(dataModel);
            for (String field : tableView.getFields()) {
                outerSelect.put(field, SemanticNode.parse(alias + "." + field, scope, engineType));
            }
            if (left == null) {
                left = SemanticNode.buildAs(tableView.getAlias(), getTable(tableView));
            } else {
                left = buildJoin(left, leftTable, tableView, beforeModels, dataModel, schema,
                        scope);
            }
            leftTable = tableView;
            beforeModels.put(dataModel.getName(), leftTable.getAlias());
        }

        for (Map.Entry<String, SqlNode> entry : outerSelect.entrySet()) {
            tableView.getSelect().add(entry.getValue());
        }
        tableView.setTable(left);
    }

    private SqlNode getTable(TableView tableView) {
        return SemanticNode.getTable(tableView.getTable());
    }

    private SqlNode buildJoin(SqlNode leftNode, TableView leftTable, TableView rightTable,
            Map<String, String> before, DataModel dataModel, S2CalciteSchema schema,
            SqlValidatorScope scope) throws Exception {
        EngineType engineType = schema.getOntology().getDatabase().getType();
        SqlNode condition =
                getCondition(leftTable, rightTable, dataModel, schema, scope, engineType);
        SqlLiteral sqlLiteral = SemanticNode.getJoinSqlLiteral("");
        JoinRelation matchJoinRelation = getMatchJoinRelation(before, rightTable, schema);
        SqlNode joinRelationCondition;
        if (!CollectionUtils.isEmpty(matchJoinRelation.getJoinCondition())) {
            sqlLiteral = SemanticNode.getJoinSqlLiteral(matchJoinRelation.getJoinType());
            joinRelationCondition = getCondition(matchJoinRelation, scope, engineType);
            condition = joinRelationCondition;
        }

        return new SqlJoin(SqlParserPos.ZERO, leftNode,
                SqlLiteral.createBoolean(false, SqlParserPos.ZERO), sqlLiteral,
                SemanticNode.buildAs(rightTable.getAlias(), getTable(rightTable)),
                SqlLiteral.createSymbol(JoinConditionType.ON, SqlParserPos.ZERO), condition);
    }

    private JoinRelation getMatchJoinRelation(Map<String, String> before, TableView tableView,
            S2CalciteSchema schema) {
        JoinRelation matchJoinRelation = JoinRelation.builder().build();
        if (!CollectionUtils.isEmpty(schema.getJoinRelations())) {
            for (JoinRelation joinRelation : schema.getJoinRelations()) {
                if (joinRelation.getRight().equalsIgnoreCase(tableView.getDataModel().getName())
                        && before.containsKey(joinRelation.getLeft())) {
                    matchJoinRelation.setJoinCondition(joinRelation.getJoinCondition().stream()
                            .map(r -> Triple.of(
                                    before.get(joinRelation.getLeft()) + "." + r.getLeft(),
                                    r.getMiddle(), tableView.getAlias() + "." + r.getRight()))
                            .collect(Collectors.toList()));
                    matchJoinRelation.setJoinType(joinRelation.getJoinType());
                    // Added join condition judgment to solve the problem of join condition order
                } else if (joinRelation.getLeft()
                        .equalsIgnoreCase(tableView.getDataModel().getName())
                        && before.containsKey(joinRelation.getRight())) {
                    matchJoinRelation.setJoinCondition(joinRelation.getJoinCondition().stream()
                            .map(r -> Triple.of(
                                    before.get(joinRelation.getRight()) + "." + r.getRight(),
                                    r.getMiddle(), tableView.getAlias() + "." + r.getLeft()))
                            .collect(Collectors.toList()));
                    matchJoinRelation.setJoinType(joinRelation.getJoinType());
                }
            }
        }
        return matchJoinRelation;
    }

    private SqlNode getCondition(JoinRelation joinRelation, SqlValidatorScope scope,
            EngineType engineType) throws Exception {
        SqlNode condition = null;
        for (Triple<String, String, String> con : joinRelation.getJoinCondition()) {
            List<SqlNode> ons = new ArrayList<>();
            ons.add(SemanticNode.parse(con.getLeft(), scope, engineType));
            ons.add(SemanticNode.parse(con.getRight(), scope, engineType));
            if (Objects.isNull(condition)) {
                condition = new SqlBasicCall(SemanticNode.getBinaryOperator(con.getMiddle()), ons,
                        SqlParserPos.ZERO, null);
                continue;
            }
            SqlNode addCondition = new SqlBasicCall(SemanticNode.getBinaryOperator(con.getMiddle()),
                    ons, SqlParserPos.ZERO, null);
            condition = new SqlBasicCall(SqlStdOperatorTable.AND,
                    new ArrayList<>(Arrays.asList(condition, addCondition)), SqlParserPos.ZERO,
                    null);
        }
        return condition;
    }

    private SqlNode getCondition(TableView left, TableView right, DataModel dataModel,
            S2CalciteSchema schema, SqlValidatorScope scope, EngineType engineType)
            throws Exception {

        Set<String> selectLeft = SemanticNode.getSelect(left.getTable());
        Set<String> selectRight = SemanticNode.getSelect(right.getTable());
        selectLeft.retainAll(selectRight);
        SqlNode condition = null;
        for (String on : selectLeft) {
            if (!isDimension(on, dataModel, schema)) {
                continue;
            }
            if (IdentifyNode.isForeign(on, left.getDataModel().getIdentifiers())) {
                if (!IdentifyNode.isPrimary(on, right.getDataModel().getIdentifiers())) {
                    continue;
                }
            }
            if (IdentifyNode.isForeign(on, right.getDataModel().getIdentifiers())) {
                if (!IdentifyNode.isPrimary(on, left.getDataModel().getIdentifiers())) {
                    continue;
                }
            }
            List<SqlNode> ons = new ArrayList<>();
            ons.add(SemanticNode.parse(left.getAlias() + "." + on, scope, engineType));
            ons.add(SemanticNode.parse(right.getAlias() + "." + on, scope, engineType));
            if (condition == null) {
                condition =
                        new SqlBasicCall(SqlStdOperatorTable.EQUALS, ons, SqlParserPos.ZERO, null);
                continue;
            }
            SqlNode addCondition =
                    new SqlBasicCall(SqlStdOperatorTable.EQUALS, ons, SqlParserPos.ZERO, null);
            condition = new SqlBasicCall(SqlStdOperatorTable.AND,
                    new ArrayList<>(Arrays.asList(condition, addCondition)), SqlParserPos.ZERO,
                    null);
        }
        return condition;
    }


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

}
