package com.tencent.supersonic.headless.core.translator.parser.calcite.render;

import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.headless.core.pojo.JoinRelation;
import com.tencent.supersonic.headless.core.pojo.OntologyQuery;
import com.tencent.supersonic.headless.core.translator.parser.calcite.S2CalciteSchema;
import com.tencent.supersonic.headless.core.translator.parser.calcite.TableView;
import com.tencent.supersonic.headless.core.translator.parser.calcite.node.AggFunctionNode;
import com.tencent.supersonic.headless.core.translator.parser.calcite.node.DataModelNode;
import com.tencent.supersonic.headless.core.translator.parser.calcite.node.FilterNode;
import com.tencent.supersonic.headless.core.translator.parser.calcite.node.IdentifyNode;
import com.tencent.supersonic.headless.core.translator.parser.calcite.node.MetricNode;
import com.tencent.supersonic.headless.core.translator.parser.calcite.node.SemanticNode;
import com.tencent.supersonic.headless.core.translator.parser.s2sql.Constants;
import com.tencent.supersonic.headless.core.translator.parser.s2sql.DataModel;
import com.tencent.supersonic.headless.core.translator.parser.s2sql.Dimension;
import com.tencent.supersonic.headless.core.translator.parser.s2sql.Identify;
import com.tencent.supersonic.headless.core.translator.parser.s2sql.Materialization;
import com.tencent.supersonic.headless.core.translator.parser.s2sql.Metric;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.JoinConditionType;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.validate.SqlValidatorScope;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/** process the join conditions when the source number is greater than 1 */
@Slf4j
public class JoinRender extends Renderer {

    @Override
    public void render(OntologyQuery metricCommand, List<DataModel> dataModels,
            SqlValidatorScope scope, S2CalciteSchema schema, boolean nonAgg) throws Exception {
        String queryWhere = metricCommand.getWhere();
        EngineType engineType = schema.getOntology().getDatabaseType();
        Set<String> whereFields = new HashSet<>();
        List<String> fieldWhere = new ArrayList<>();
        if (queryWhere != null && !queryWhere.isEmpty()) {
            SqlNode sqlNode = SemanticNode.parse(queryWhere, scope, engineType);
            FilterNode.getFilterField(sqlNode, whereFields);
            fieldWhere = whereFields.stream().collect(Collectors.toList());
        }
        Set<String> queryAllDimension = new HashSet<>();
        Set<String> measures = new HashSet<>();
        DataModelNode.getQueryDimensionMeasure(schema.getOntology(), metricCommand,
                queryAllDimension, measures);
        SqlNode left = null;
        TableView leftTable = null;
        TableView innerView = new TableView();
        TableView filterView = new TableView();
        Map<String, SqlNode> innerSelect = new HashMap<>();
        Set<String> filterDimension = new HashSet<>();
        Map<String, String> beforeSources = new HashMap<>();

        for (int i = 0; i < dataModels.size(); i++) {
            final DataModel dataModel = dataModels.get(i);
            final Set<String> filterDimensions = new HashSet<>();
            final Set<String> filterMetrics = new HashSet<>();
            final Set<String> queryDimension = new HashSet<>();
            final Set<String> queryMetrics = new HashSet<>();
            SourceRender.whereDimMetric(fieldWhere, queryMetrics, queryDimension, dataModel, schema,
                    filterDimensions, filterMetrics);
            List<String> reqMetric = new ArrayList<>(metricCommand.getMetrics());
            reqMetric.addAll(filterMetrics);
            reqMetric = uniqList(reqMetric);

            List<String> reqDimension = new ArrayList<>(metricCommand.getDimensions());
            reqDimension.addAll(filterDimensions);
            reqDimension = uniqList(reqDimension);

            Set<String> sourceMeasure = dataModel.getMeasures().stream().map(mm -> mm.getName())
                    .collect(Collectors.toSet());
            doMetric(innerSelect, filterView, queryMetrics, reqMetric, dataModel, sourceMeasure,
                    scope, schema, nonAgg);
            Set<String> dimension = dataModel.getDimensions().stream().map(dd -> dd.getName())
                    .collect(Collectors.toSet());
            doDimension(innerSelect, filterDimension, queryDimension, reqDimension, dataModel,
                    dimension, scope, schema);
            List<String> primary = new ArrayList<>();
            for (Identify identify : dataModel.getIdentifiers()) {
                primary.add(identify.getName());
                if (!fieldWhere.contains(identify.getName())) {
                    fieldWhere.add(identify.getName());
                }
            }
            List<String> dataSourceWhere = new ArrayList<>(fieldWhere);
            addZipperField(dataModel, dataSourceWhere);
            TableView tableView =
                    SourceRender.renderOne("", dataSourceWhere, queryMetrics, queryDimension,
                            metricCommand.getWhere(), dataModels.get(i), scope, schema, true);
            log.info("tableView {}", StringUtils.normalizeSpace(tableView.getTable().toString()));
            String alias = Constants.JOIN_TABLE_PREFIX + dataModel.getName();
            tableView.setAlias(alias);
            tableView.setPrimary(primary);
            tableView.setDataModel(dataModel);
            if (left == null) {
                leftTable = tableView;
                left = SemanticNode.buildAs(tableView.getAlias(), getTable(tableView, scope));
                beforeSources.put(dataModel.getName(), leftTable.getAlias());
                continue;
            }
            left = buildJoin(left, leftTable, tableView, beforeSources, dataModel, schema, scope);
            leftTable = tableView;
            beforeSources.put(dataModel.getName(), tableView.getAlias());
        }

        for (Map.Entry<String, SqlNode> entry : innerSelect.entrySet()) {
            innerView.getMeasure().add(entry.getValue());
        }
        innerView.setTable(left);
        filterView
                .setTable(SemanticNode.buildAs(Constants.JOIN_TABLE_OUT_PREFIX, innerView.build()));
        if (!filterDimension.isEmpty()) {
            for (String d : getQueryDimension(filterDimension, queryAllDimension, whereFields)) {
                if (nonAgg) {
                    filterView.getMeasure().add(SemanticNode.parse(d, scope, engineType));
                } else {
                    filterView.getDimension().add(SemanticNode.parse(d, scope, engineType));
                }
            }
        }
        filterView.setMeasure(SemanticNode.deduplicateNode(filterView.getMeasure()));
        filterView.setDimension(SemanticNode.deduplicateNode(filterView.getDimension()));
        super.tableView = filterView;
    }

    private void doMetric(Map<String, SqlNode> innerSelect, TableView filterView,
            Set<String> queryMetrics, List<String> reqMetrics, DataModel dataModel,
            Set<String> sourceMeasure, SqlValidatorScope scope, S2CalciteSchema schema,
            boolean nonAgg) throws Exception {
        String alias = Constants.JOIN_TABLE_PREFIX + dataModel.getName();
        EngineType engineType = schema.getOntology().getDatabaseType();
        for (String m : reqMetrics) {
            if (getMatchMetric(schema, sourceMeasure, m, queryMetrics)) {
                MetricNode metricNode = buildMetricNode(m, dataModel, scope, schema, nonAgg, alias);

                if (!metricNode.getNonAggNode().isEmpty()) {
                    for (String measure : metricNode.getNonAggNode().keySet()) {
                        innerSelect.put(measure, SemanticNode.buildAs(measure,
                                SemanticNode.parse(alias + "." + measure, scope, engineType)));
                    }
                }
                if (metricNode.getAggFunction() != null && !metricNode.getAggFunction().isEmpty()) {
                    for (Map.Entry<String, String> entry : metricNode.getAggFunction().entrySet()) {
                        if (metricNode.getNonAggNode().containsKey(entry.getKey())) {
                            if (nonAgg) {
                                filterView.getMeasure().add(SemanticNode.buildAs(entry.getKey(),
                                        SemanticNode.parse(entry.getKey(), scope, engineType)));
                            } else {
                                filterView.getMeasure()
                                        .add(SemanticNode.buildAs(entry.getKey(),
                                                AggFunctionNode.build(entry.getValue(),
                                                        entry.getKey(), scope, engineType)));
                            }
                        }
                    }
                }
            }
        }
    }

    private void doDimension(Map<String, SqlNode> innerSelect, Set<String> filterDimension,
            Set<String> queryDimension, List<String> reqDimensions, DataModel dataModel,
            Set<String> dimension, SqlValidatorScope scope, S2CalciteSchema schema)
            throws Exception {
        String alias = Constants.JOIN_TABLE_PREFIX + dataModel.getName();
        EngineType engineType = schema.getOntology().getDatabaseType();
        for (String d : reqDimensions) {
            if (getMatchDimension(schema, dimension, dataModel, d, queryDimension)) {
                if (d.contains(Constants.DIMENSION_IDENTIFY)) {
                    String[] identifyDimension = d.split(Constants.DIMENSION_IDENTIFY);
                    innerSelect.put(d, SemanticNode.buildAs(d, SemanticNode
                            .parse(alias + "." + identifyDimension[1], scope, engineType)));
                } else {
                    innerSelect.put(d, SemanticNode.buildAs(d,
                            SemanticNode.parse(alias + "." + d, scope, engineType)));
                }
                filterDimension.add(d);
            }
        }
    }

    private Set<String> getQueryDimension(Set<String> filterDimension,
            Set<String> queryAllDimension, Set<String> whereFields) {
        return filterDimension.stream()
                .filter(d -> queryAllDimension.contains(d) || whereFields.contains(d))
                .collect(Collectors.toSet());
    }

    private boolean getMatchMetric(S2CalciteSchema schema, Set<String> sourceMeasure, String m,
            Set<String> queryMetrics) {
        Optional<Metric> metric = schema.getMetrics().stream()
                .filter(mm -> mm.getName().equalsIgnoreCase(m)).findFirst();
        boolean isAdd = false;
        if (metric.isPresent()) {
            Set<String> metricMeasures = metric.get().getMetricTypeParams().getMeasures().stream()
                    .map(me -> me.getName()).collect(Collectors.toSet());
            if (sourceMeasure.containsAll(metricMeasures)) {
                isAdd = true;
            }
        }
        if (sourceMeasure.contains(m)) {
            isAdd = true;
        }
        if (isAdd && !queryMetrics.contains(m)) {
            queryMetrics.add(m);
        }
        return isAdd;
    }

    private boolean getMatchDimension(S2CalciteSchema schema, Set<String> sourceDimension,
            DataModel dataModel, String d, Set<String> queryDimension) {
        String oriDimension = d;
        boolean isAdd = false;
        if (d.contains(Constants.DIMENSION_IDENTIFY)) {
            oriDimension = d.split(Constants.DIMENSION_IDENTIFY)[1];
        }
        if (sourceDimension.contains(oriDimension)) {
            isAdd = true;
        }
        for (Identify identify : dataModel.getIdentifiers()) {
            if (identify.getName().equalsIgnoreCase(oriDimension)) {
                isAdd = true;
                break;
            }
        }
        if (schema.getDimensions().containsKey(dataModel.getName())) {
            for (Dimension dim : schema.getDimensions().get(dataModel.getName())) {
                if (dim.getName().equalsIgnoreCase(oriDimension)) {
                    isAdd = true;
                }
            }
        }
        if (isAdd && !queryDimension.contains(oriDimension)) {
            queryDimension.add(oriDimension);
        }
        return isAdd;
    }

    private SqlNode getTable(TableView tableView, SqlValidatorScope scope) throws Exception {
        return SemanticNode.getTable(tableView.getTable());
    }

    private SqlNode buildJoin(SqlNode left, TableView leftTable, TableView tableView,
            Map<String, String> before, DataModel dataModel, S2CalciteSchema schema,
            SqlValidatorScope scope) throws Exception {
        EngineType engineType = schema.getOntology().getDatabaseType();
        SqlNode condition =
                getCondition(leftTable, tableView, dataModel, schema, scope, engineType);
        SqlLiteral sqlLiteral = SemanticNode.getJoinSqlLiteral("");
        JoinRelation matchJoinRelation = getMatchJoinRelation(before, tableView, schema);
        SqlNode joinRelationCondition = null;
        if (!CollectionUtils.isEmpty(matchJoinRelation.getJoinCondition())) {
            sqlLiteral = SemanticNode.getJoinSqlLiteral(matchJoinRelation.getJoinType());
            joinRelationCondition = getCondition(matchJoinRelation, scope, engineType);
            condition = joinRelationCondition;
        }
        if (Materialization.TimePartType.ZIPPER.equals(leftTable.getDataModel().getTimePartType())
                || Materialization.TimePartType.ZIPPER
                        .equals(tableView.getDataModel().getTimePartType())) {
            SqlNode zipperCondition =
                    getZipperCondition(leftTable, tableView, dataModel, schema, scope);
            if (Objects.nonNull(joinRelationCondition)) {
                condition = new SqlBasicCall(SqlStdOperatorTable.AND,
                        new ArrayList<>(Arrays.asList(zipperCondition, joinRelationCondition)),
                        SqlParserPos.ZERO, null);
            } else {
                condition = zipperCondition;
            }
        }

        return new SqlJoin(SqlParserPos.ZERO, left,
                SqlLiteral.createBoolean(false, SqlParserPos.ZERO), sqlLiteral,
                SemanticNode.buildAs(tableView.getAlias(), getTable(tableView, scope)),
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
        if (Materialization.TimePartType.ZIPPER.equals(dataModel.getTimePartType())) {
            dataModel.getDimensions().stream()
                    .filter(d -> Constants.DIMENSION_TYPE_TIME.equalsIgnoreCase(d.getType()))
                    .forEach(t -> {
                        if (t.getName().startsWith(Constants.MATERIALIZATION_ZIPPER_END)
                                && !fields.contains(t.getName())) {
                            fields.add(t.getName());
                        }
                        if (t.getName().startsWith(Constants.MATERIALIZATION_ZIPPER_START)
                                && !fields.contains(t.getName())) {
                            fields.add(t.getName());
                        }
                    });
        }
    }

    private SqlNode getZipperCondition(TableView left, TableView right, DataModel dataModel,
            S2CalciteSchema schema, SqlValidatorScope scope) throws Exception {
        if (Materialization.TimePartType.ZIPPER.equals(left.getDataModel().getTimePartType())
                && Materialization.TimePartType.ZIPPER
                        .equals(right.getDataModel().getTimePartType())) {
            throw new Exception("not support two zipper table");
        }
        SqlNode condition = null;
        Optional<Dimension> leftTime = left.getDataModel().getDimensions().stream()
                .filter(d -> Constants.DIMENSION_TYPE_TIME.equalsIgnoreCase(d.getType()))
                .findFirst();
        Optional<Dimension> rightTime = right.getDataModel().getDimensions().stream()
                .filter(d -> Constants.DIMENSION_TYPE_TIME.equalsIgnoreCase(d.getType()))
                .findFirst();
        if (leftTime.isPresent() && rightTime.isPresent()) {

            String startTime = "";
            String endTime = "";
            String dateTime = "";

            Optional<Dimension> startTimeOp = (Materialization.TimePartType.ZIPPER
                    .equals(left.getDataModel().getTimePartType()) ? left : right).getDataModel()
                            .getDimensions().stream()
                            .filter(d -> Constants.DIMENSION_TYPE_TIME
                                    .equalsIgnoreCase(d.getType()))
                            .filter(d -> d.getName()
                                    .startsWith(Constants.MATERIALIZATION_ZIPPER_START))
                            .findFirst();
            Optional<Dimension> endTimeOp = (Materialization.TimePartType.ZIPPER
                    .equals(left.getDataModel().getTimePartType()) ? left : right).getDataModel()
                            .getDimensions().stream()
                            .filter(d -> Constants.DIMENSION_TYPE_TIME
                                    .equalsIgnoreCase(d.getType()))
                            .filter(d -> d.getName()
                                    .startsWith(Constants.MATERIALIZATION_ZIPPER_END))
                            .findFirst();
            if (startTimeOp.isPresent() && endTimeOp.isPresent()) {
                TableView zipper = Materialization.TimePartType.ZIPPER
                        .equals(left.getDataModel().getTimePartType()) ? left : right;
                TableView partMetric = Materialization.TimePartType.ZIPPER
                        .equals(left.getDataModel().getTimePartType()) ? right : left;
                Optional<Dimension> partTime = Materialization.TimePartType.ZIPPER
                        .equals(left.getDataModel().getTimePartType()) ? rightTime : leftTime;
                startTime = zipper.getAlias() + "." + startTimeOp.get().getName();
                endTime = zipper.getAlias() + "." + endTimeOp.get().getName();
                dateTime = partMetric.getAlias() + "." + partTime.get().getName();
            }
            EngineType engineType = schema.getOntology().getDatabaseType();
            ArrayList<SqlNode> operandList =
                    new ArrayList<>(Arrays.asList(SemanticNode.parse(endTime, scope, engineType),
                            SemanticNode.parse(dateTime, scope, engineType)));
            condition = new SqlBasicCall(SqlStdOperatorTable.AND,
                    new ArrayList<SqlNode>(Arrays.asList(
                            new SqlBasicCall(SqlStdOperatorTable.LESS_THAN_OR_EQUAL,
                                    new ArrayList<SqlNode>(Arrays.asList(
                                            SemanticNode.parse(startTime, scope, engineType),
                                            SemanticNode.parse(dateTime, scope, engineType))),
                                    SqlParserPos.ZERO, null),
                            new SqlBasicCall(SqlStdOperatorTable.GREATER_THAN, operandList,
                                    SqlParserPos.ZERO, null))),
                    SqlParserPos.ZERO, null);
        }
        return condition;
    }
}
