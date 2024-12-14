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
import com.tencent.supersonic.headless.core.translator.parser.calcite.node.IdentifyNode;
import com.tencent.supersonic.headless.core.translator.parser.calcite.node.SemanticNode;
import com.tencent.supersonic.headless.core.translator.parser.s2sql.Constants;
import com.tencent.supersonic.headless.core.translator.parser.s2sql.Materialization;
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
            SqlValidatorScope scope, S2CalciteSchema schema, boolean nonAgg) throws Exception {
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

            TableView tableView = SourceRender.renderOne(queryMetrics, queryDimensions, dataModel, scope, schema);
            log.info("tableView {}", StringUtils.normalizeSpace(tableView.getTable().toString()));
            String alias = Constants.JOIN_TABLE_PREFIX + dataModel.getName();
            tableView.setAlias(alias);
            tableView.setPrimary(primary);
            tableView.setDataModel(dataModel);
            for (String field : tableView.getFields()) {
                outerSelect.put(field, SemanticNode.parse(alias + "." + field, scope, engineType));
            }
            if (left == null) {
                leftTable = tableView;
                left = SemanticNode.buildAs(tableView.getAlias(), getTable(tableView));
                beforeModels.put(dataModel.getName(), leftTable.getAlias());
                continue;
            }
            left = buildJoin(left, leftTable, tableView, beforeModels, dataModel, schema, scope);
            leftTable = tableView;
            beforeModels.put(dataModel.getName(), tableView.getAlias());
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
        SqlNode condition = getCondition(leftTable, rightTable, dataModel, schema, scope, engineType);
        SqlLiteral sqlLiteral = SemanticNode.getJoinSqlLiteral("");
        JoinRelation matchJoinRelation = getMatchJoinRelation(before, rightTable, schema);
        SqlNode joinRelationCondition = null;
        if (!CollectionUtils.isEmpty(matchJoinRelation.getJoinCondition())) {
            sqlLiteral = SemanticNode.getJoinSqlLiteral(matchJoinRelation.getJoinType());
            joinRelationCondition = getCondition(matchJoinRelation, scope, engineType);
            condition = joinRelationCondition;
        }
        if (Materialization.TimePartType.ZIPPER.equals(leftTable.getDataModel().getTimePartType())
                || Materialization.TimePartType.ZIPPER
                        .equals(rightTable.getDataModel().getTimePartType())) {
            SqlNode zipperCondition =
                    getZipperCondition(leftTable, rightTable, dataModel, schema, scope);
            if (Objects.nonNull(joinRelationCondition)) {
                condition = new SqlBasicCall(SqlStdOperatorTable.AND,
                        new ArrayList<>(Arrays.asList(zipperCondition, joinRelationCondition)),
                        SqlParserPos.ZERO, null);
            } else {
                condition = zipperCondition;
            }
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
            if (!SourceRender.isDimension(on, dataModel, schema)) {
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

    private static void joinOrder(int cnt, String id, Map<String, Set<String>> next,
            Queue<String> orders, Map<String, Boolean> visited) {
        visited.put(id, true);
        orders.add(id);
        if (orders.size() >= cnt) {
            return;
        }
        for (String nextId : next.get(id)) {
            if (!visited.get(nextId)) {
                joinOrder(cnt, nextId, next, orders, visited);
                if (orders.size() >= cnt) {
                    return;
                }
            }
        }
        orders.poll();
        visited.put(id, false);
    }

    private void addZipperField(DataModel dataModel, List<String> fields) {
        // if (Materialization.TimePartType.ZIPPER.equals(dataModel.getTimePartType())) {
        // dataModel.getDimensions().stream()
        // .filter(d -> Constants.DIMENSION_TYPE_TIME.equalsIgnoreCase(d.getType()))
        // .forEach(t -> {
        // if (t.getName().startsWith(Constants.MATERIALIZATION_ZIPPER_END)
        // && !fields.contains(t.getName())) {
        // fields.add(t.getName());
        // }
        // if (t.getName().startsWith(Constants.MATERIALIZATION_ZIPPER_START)
        // && !fields.contains(t.getName())) {
        // fields.add(t.getName());
        // }
        // });
        // }
    }

    private SqlNode getZipperCondition(TableView left, TableView right, DataModel dataModel,
            S2CalciteSchema schema, SqlValidatorScope scope) throws Exception {
        // if (Materialization.TimePartType.ZIPPER.equals(left.getDataModel().getTimePartType())
        // && Materialization.TimePartType.ZIPPER
        // .equals(right.getDataModel().getTimePartType())) {
        // throw new Exception("not support two zipper table");
        // }
        SqlNode condition = null;
        // Optional<Dimension> leftTime = left.getDataModel().getDimensions().stream()
        // .filter(d -> Constants.DIMENSION_TYPE_TIME.equalsIgnoreCase(d.getType()))
        // .findFirst();
        // Optional<Dimension> rightTime = right.getDataModel().getDimensions().stream()
        // .filter(d -> Constants.DIMENSION_TYPE_TIME.equalsIgnoreCase(d.getType()))
        // .findFirst();
        // if (leftTime.isPresent() && rightTime.isPresent()) {
        //
        // String startTime = "";
        // String endTime = "";
        // String dateTime = "";
        //
        // Optional<Dimension> startTimeOp = (Materialization.TimePartType.ZIPPER
        // .equals(left.getDataModel().getTimePartType()) ? left : right).getDataModel()
        // .getDimensions().stream()
        // .filter(d -> Constants.DIMENSION_TYPE_TIME
        // .equalsIgnoreCase(d.getType()))
        // .filter(d -> d.getName()
        // .startsWith(Constants.MATERIALIZATION_ZIPPER_START))
        // .findFirst();
        // Optional<Dimension> endTimeOp = (Materialization.TimePartType.ZIPPER
        // .equals(left.getDataModel().getTimePartType()) ? left : right).getDataModel()
        // .getDimensions().stream()
        // .filter(d -> Constants.DIMENSION_TYPE_TIME
        // .equalsIgnoreCase(d.getType()))
        // .filter(d -> d.getName()
        // .startsWith(Constants.MATERIALIZATION_ZIPPER_END))
        // .findFirst();
        // if (startTimeOp.isPresent() && endTimeOp.isPresent()) {
        // TableView zipper = Materialization.TimePartType.ZIPPER
        // .equals(left.getDataModel().getTimePartType()) ? left : right;
        // TableView partMetric = Materialization.TimePartType.ZIPPER
        // .equals(left.getDataModel().getTimePartType()) ? right : left;
        // Optional<Dimension> partTime = Materialization.TimePartType.ZIPPER
        // .equals(left.getDataModel().getTimePartType()) ? rightTime : leftTime;
        // startTime = zipper.getAlias() + "." + startTimeOp.get().getName();
        // endTime = zipper.getAlias() + "." + endTimeOp.get().getName();
        // dateTime = partMetric.getAlias() + "." + partTime.get().getName();
        // }
        // EngineType engineType = schema.getOntology().getDatabase().getType();
        // ArrayList<SqlNode> operandList =
        // new ArrayList<>(Arrays.asList(SemanticNode.parse(endTime, scope, engineType),
        // SemanticNode.parse(dateTime, scope, engineType)));
        // condition = new SqlBasicCall(SqlStdOperatorTable.AND,
        // new ArrayList<SqlNode>(Arrays.asList(
        // new SqlBasicCall(SqlStdOperatorTable.LESS_THAN_OR_EQUAL,
        // new ArrayList<SqlNode>(Arrays.asList(
        // SemanticNode.parse(startTime, scope, engineType),
        // SemanticNode.parse(dateTime, scope, engineType))),
        // SqlParserPos.ZERO, null),
        // new SqlBasicCall(SqlStdOperatorTable.GREATER_THAN, operandList,
        // SqlParserPos.ZERO, null))),
        // SqlParserPos.ZERO, null);
        // }
        return condition;
    }
}
