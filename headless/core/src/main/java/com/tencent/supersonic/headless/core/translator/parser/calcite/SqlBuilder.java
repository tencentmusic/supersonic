package com.tencent.supersonic.headless.core.translator.parser.calcite;

import com.tencent.supersonic.common.calcite.Configuration;
import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.headless.api.pojo.Dimension;
import com.tencent.supersonic.headless.api.pojo.Identify;
import com.tencent.supersonic.headless.api.pojo.enums.IdentifyType;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.core.pojo.*;
import com.tencent.supersonic.headless.core.translator.parser.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.validate.SqlValidatorScope;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class SqlBuilder {

    private final S2CalciteSchema schema;
    private final SqlValidatorScope scope;

    public SqlBuilder(S2CalciteSchema schema) {
        this.schema = schema;
        this.scope = SchemaBuilder.getScope(schema);
    }

    public String buildOntologySql(QueryStatement queryStatement) throws Exception {
        OntologyQuery ontologyQuery = queryStatement.getOntologyQuery();
        if (ontologyQuery.getLimit() == null) {
            ontologyQuery.setLimit(0L);
        }

        Set<ModelResp> dataModels = ontologyQuery.getModels();
        if (dataModels == null || dataModels.isEmpty()) {
            throw new Exception("data model not found");
        }

        TableView tableView = render(ontologyQuery, new ArrayList<>(dataModels), scope, schema);
        SqlNode parserNode = tableView.build();
        DatabaseResp database = queryStatement.getOntology().getDatabase();
        EngineType engineType = EngineType.fromString(database.getType());
        parserNode = optimizeParseNode(parserNode, engineType);
        return SemanticNode.getSql(parserNode, engineType);
    }

    private SqlNode optimizeParseNode(SqlNode parserNode, EngineType engineType) {
        if (Objects.isNull(schema.getRuntimeOptions())
                || Objects.isNull(schema.getRuntimeOptions().getEnableOptimize())
                || !schema.getRuntimeOptions().getEnableOptimize()) {
            return parserNode;
        }

        SqlNode optimizeNode = null;
        try {
            SqlNode sqlNode = SqlParser.create(SemanticNode.getSql(parserNode, engineType),
                    Configuration.getParserConfig(engineType)).parseStmt();
            if (Objects.nonNull(sqlNode)) {
                optimizeNode = SemanticNode.optimize(scope, schema, sqlNode, engineType);
            }
        } catch (Exception e) {
            log.error("optimize error {}", e);
        }

        if (Objects.nonNull(optimizeNode)) {
            return optimizeNode;
        }

        return parserNode;
    }

    private TableView render(OntologyQuery ontologyQuery, List<ModelResp> dataModels,
            SqlValidatorScope scope, S2CalciteSchema schema) throws Exception {
        SqlNode left = null;
        TableView leftTable = null;
        TableView outerTable = new TableView();
        Map<String, SqlNode> outerSelect = new HashMap<>();
        Map<String, String> beforeModels = new HashMap<>();
        EngineType engineType = EngineType.fromString(schema.getOntology().getDatabase().getType());

        for (int i = 0; i < dataModels.size(); i++) {
            final ModelResp dataModel = dataModels.get(i);
            final Set<DimSchemaResp> queryDimensions =
                    ontologyQuery.getDimensionsByModel(dataModel.getName());
            final Set<MetricSchemaResp> queryMetrics =
                    ontologyQuery.getMetricsByModel(dataModel.getName());

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
            outerTable.getSelect().add(entry.getValue());
        }
        outerTable.setTable(left);

        return outerTable;
    }

    private SqlNode getTable(TableView tableView) {
        return SemanticNode.getTable(tableView.getTable());
    }

    private SqlNode buildJoin(SqlNode leftNode, TableView leftTable, TableView rightTable,
            Map<String, String> before, ModelResp dataModel, S2CalciteSchema schema,
            SqlValidatorScope scope) throws Exception {
        EngineType engineType = EngineType.fromString(schema.getOntology().getDatabase().getType());
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

    private SqlNode getCondition(TableView left, TableView right, ModelResp dataModel,
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
            if (isForeign(on, left.getDataModel().getIdentifiers())) {
                if (!isPrimary(on, right.getDataModel().getIdentifiers())) {
                    continue;
                }
            }
            if (isForeign(on, right.getDataModel().getIdentifiers())) {
                if (!isPrimary(on, left.getDataModel().getIdentifiers())) {
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
            Set<DimSchemaResp> queryDimensions, ModelResp dataModel, SqlValidatorScope scope,
            S2CalciteSchema schema) {
        TableView tableView = new TableView();
        EngineType engineType = EngineType.fromString(schema.getOntology().getDatabase().getType());
        Set<String> queryFields = tableView.getFields();
        if (Objects.nonNull(queryMetrics)) {
            queryMetrics.stream().forEach(m -> queryFields.addAll(m.getFields()));
        }
        if (Objects.nonNull(queryDimensions)) {
            queryDimensions.stream().forEach(d -> queryFields.addAll(d.getFields()));
        }

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

    private static boolean isDimension(String name, ModelResp dataModel, S2CalciteSchema schema) {
        Optional<Dimension> dimension = dataModel.getModelDetail().getDimensions().stream()
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

    private static boolean isForeign(String name, List<Identify> identifies) {
        Optional<Identify> identify =
                identifies.stream().filter(i -> i.getName().equalsIgnoreCase(name)).findFirst();
        if (identify.isPresent()) {
            return IdentifyType.foreign.equals(identify.get().getType());
        }
        return false;
    }

    private static boolean isPrimary(String name, List<Identify> identifies) {
        Optional<Identify> identify =
                identifies.stream().filter(i -> i.getName().equalsIgnoreCase(name)).findFirst();
        if (identify.isPresent()) {
            return IdentifyType.primary.equals(identify.get().getType());
        }
        return false;
    }

}
