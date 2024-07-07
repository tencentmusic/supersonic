package com.tencent.supersonic.headless.core.translator.calcite.sql.render;

import com.tencent.supersonic.headless.api.pojo.enums.EngineType;
import com.tencent.supersonic.headless.core.pojo.MetricQueryParam;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Identify;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Materialization;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Metric;
import com.tencent.supersonic.headless.core.translator.calcite.schema.SemanticSchema;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Constants;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.DataSource;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Dimension;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.JoinRelation;
import com.tencent.supersonic.headless.core.translator.calcite.sql.Renderer;
import com.tencent.supersonic.headless.core.translator.calcite.sql.TableView;
import com.tencent.supersonic.headless.core.translator.calcite.sql.node.AggFunctionNode;
import com.tencent.supersonic.headless.core.translator.calcite.sql.node.DataSourceNode;
import com.tencent.supersonic.headless.core.translator.calcite.sql.node.FilterNode;
import com.tencent.supersonic.headless.core.translator.calcite.sql.node.IdentifyNode;
import com.tencent.supersonic.headless.core.translator.calcite.sql.node.MetricNode;
import com.tencent.supersonic.headless.core.translator.calcite.sql.node.SemanticNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.JoinConditionType;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.validate.SqlValidatorScope;
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

/**
 * process the join conditions when the source number is greater than 1
 */
@Slf4j
public class JoinRender extends Renderer {

    @Override
    public void render(MetricQueryParam metricCommand, List<DataSource> dataSources, SqlValidatorScope scope,
                       SemanticSchema schema, boolean nonAgg) throws Exception {
        String queryWhere = metricCommand.getWhere();
        EngineType engineType = EngineType.fromString(schema.getSemanticModel().getDatabase().getType());
        Set<String> whereFields = new HashSet<>();
        List<String> fieldWhere = new ArrayList<>();
        if (queryWhere != null && !queryWhere.isEmpty()) {
            SqlNode sqlNode = SemanticNode.parse(queryWhere, scope, engineType);
            FilterNode.getFilterField(sqlNode, whereFields);
            fieldWhere = whereFields.stream().collect(Collectors.toList());
        }
        Set<String> queryAllDimension = new HashSet<>();
        List<String> measures = new ArrayList<>();
        DataSourceNode.getQueryDimensionMeasure(schema, metricCommand, queryAllDimension, measures);
        SqlNode left = null;
        TableView leftTable = null;
        TableView innerView = new TableView();
        TableView filterView = new TableView();
        Map<String, SqlNode> innerSelect = new HashMap<>();
        Set<String> filterDimension = new HashSet<>();
        Map<String, String> beforeSources = new HashMap<>();

        for (int i = 0; i < dataSources.size(); i++) {
            final DataSource dataSource = dataSources.get(i);
            final Set<String> filterDimensions = new HashSet<>();
            final Set<String> filterMetrics = new HashSet<>();
            final List<String> queryDimension = new ArrayList<>();
            final List<String> queryMetrics = new ArrayList<>();
            SourceRender.whereDimMetric(fieldWhere, queryMetrics, queryDimension, dataSource, schema, filterDimensions,
                    filterMetrics);
            List<String> reqMetric = new ArrayList<>(metricCommand.getMetrics());
            reqMetric.addAll(filterMetrics);
            reqMetric = uniqList(reqMetric);

            List<String> reqDimension = new ArrayList<>(metricCommand.getDimensions());
            reqDimension.addAll(filterDimensions);
            reqDimension = uniqList(reqDimension);

            Set<String> sourceMeasure = dataSource.getMeasures().stream().map(mm -> mm.getName())
                    .collect(Collectors.toSet());
            doMetric(innerSelect, filterView, queryMetrics, reqMetric, dataSource, sourceMeasure, scope, schema,
                    nonAgg);
            Set<String> dimension = dataSource.getDimensions().stream().map(dd -> dd.getName())
                    .collect(Collectors.toSet());
            doDimension(innerSelect, filterDimension, queryDimension, reqDimension, dataSource, dimension, scope,
                    schema);
            List<String> primary = new ArrayList<>();
            for (Identify identify : dataSource.getIdentifiers()) {
                primary.add(identify.getName());
                if (!fieldWhere.contains(identify.getName())) {
                    fieldWhere.add(identify.getName());
                }
            }
            List<String> dataSourceWhere = new ArrayList<>(fieldWhere);
            addZipperField(dataSource, dataSourceWhere);
            TableView tableView = SourceRender.renderOne("", dataSourceWhere, queryMetrics, queryDimension,
                    metricCommand.getWhere(), dataSources.get(i), scope, schema, true);
            log.info("tableView {}", tableView.getTable().toString());
            String alias = Constants.JOIN_TABLE_PREFIX + dataSource.getName();
            tableView.setAlias(alias);
            tableView.setPrimary(primary);
            tableView.setDataSource(dataSource);
            if (left == null) {
                leftTable = tableView;
                left = SemanticNode.buildAs(tableView.getAlias(), getTable(tableView, scope));
                beforeSources.put(dataSource.getName(), leftTable.getAlias());
                continue;
            }
            left = buildJoin(left, leftTable, tableView, beforeSources, dataSource, schema, scope);
            leftTable = tableView;
            beforeSources.put(dataSource.getName(), tableView.getAlias());
        }

        for (Map.Entry<String, SqlNode> entry : innerSelect.entrySet()) {
            innerView.getMeasure().add(entry.getValue());
        }
        innerView.setTable(left);
        filterView.setTable(SemanticNode.buildAs(Constants.JOIN_TABLE_OUT_PREFIX, innerView.build()));
        if (!filterDimension.isEmpty()) {
            for (String d : getQueryDimension(filterDimension, queryAllDimension, whereFields)) {
                if (nonAgg) {
                    filterView.getMeasure().add(SemanticNode.parse(d, scope, engineType));
                } else {
                    filterView.getDimension().add(SemanticNode.parse(d, scope, engineType));
                }

            }
        }
        super.tableView = filterView;
    }

    private void doMetric(Map<String, SqlNode> innerSelect, TableView filterView, List<String> queryMetrics,
            List<String> reqMetrics, DataSource dataSource, Set<String> sourceMeasure, SqlValidatorScope scope,
            SemanticSchema schema, boolean nonAgg) throws Exception {
        String alias = Constants.JOIN_TABLE_PREFIX + dataSource.getName();
        EngineType engineType = EngineType.fromString(schema.getSemanticModel().getDatabase().getType());
        for (String m : reqMetrics) {
            if (getMatchMetric(schema, sourceMeasure, m, queryMetrics)) {
                MetricNode metricNode = buildMetricNode(m, dataSource, scope, schema, nonAgg, alias);

                if (!metricNode.getNonAggNode().isEmpty()) {
                    for (String measure : metricNode.getNonAggNode().keySet()) {
                        innerSelect.put(measure,
                                SemanticNode.buildAs(measure,
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
                                filterView.getMeasure().add(SemanticNode.buildAs(entry.getKey(),
                                        AggFunctionNode.build(entry.getValue(), entry.getKey(), scope, engineType)));
                            }

                        }
                    }

                }
            }
        }
    }

    private void doDimension(Map<String, SqlNode> innerSelect, Set<String> filterDimension, List<String> queryDimension,
            List<String> reqDimensions, DataSource dataSource, Set<String> dimension, SqlValidatorScope scope,
            SemanticSchema schema) throws Exception {
        String alias = Constants.JOIN_TABLE_PREFIX + dataSource.getName();
        EngineType engineType = EngineType.fromString(schema.getSemanticModel().getDatabase().getType());
        for (String d : reqDimensions) {
            if (getMatchDimension(schema, dimension, dataSource, d, queryDimension)) {
                if (d.contains(Constants.DIMENSION_IDENTIFY)) {
                    String[] identifyDimension = d.split(Constants.DIMENSION_IDENTIFY);
                    innerSelect.put(d,
                            SemanticNode.buildAs(d,
                                    SemanticNode.parse(alias + "." + identifyDimension[1], scope, engineType)));
                } else {
                    innerSelect.put(d, SemanticNode.buildAs(d, SemanticNode.parse(alias + "." + d, scope, engineType)));

                }
                filterDimension.add(d);
            }
        }
    }

    private Set<String> getQueryDimension(Set<String> filterDimension, Set<String> queryAllDimension,
            Set<String> whereFields) {
        return filterDimension.stream().filter(d -> queryAllDimension.contains(d) || whereFields.contains(d)).collect(
                Collectors.toSet());
    }

    private boolean getMatchMetric(SemanticSchema schema, Set<String> sourceMeasure, String m,
            List<String> queryMetrics) {
        Optional<Metric> metric = schema.getMetrics().stream().filter(mm -> mm.getName().equalsIgnoreCase(m))
                .findFirst();
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

    private boolean getMatchDimension(SemanticSchema schema, Set<String> sourceDimension, DataSource dataSource,
            String d, List<String> queryDimension) {
        String oriDimension = d;
        boolean isAdd = false;
        if (d.contains(Constants.DIMENSION_IDENTIFY)) {
            oriDimension = d.split(Constants.DIMENSION_IDENTIFY)[1];
        }
        if (sourceDimension.contains(oriDimension)) {
            isAdd = true;
        }
        for (Identify identify : dataSource.getIdentifiers()) {
            if (identify.getName().equalsIgnoreCase(oriDimension)) {
                isAdd = true;
                break;
            }
        }
        if (schema.getDimension().containsKey(dataSource.getName())) {
            for (Dimension dim : schema.getDimension().get(dataSource.getName())) {
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

    private SqlNode buildJoin(SqlNode left, TableView leftTable, TableView tableView, Map<String, String> before,
            DataSource dataSource, SemanticSchema schema, SqlValidatorScope scope)
            throws Exception {
        EngineType engineType = EngineType.fromString(schema.getSemanticModel().getDatabase().getType());
        SqlNode condition = getCondition(leftTable, tableView, dataSource, schema, scope, engineType);
        SqlLiteral sqlLiteral = SemanticNode.getJoinSqlLiteral("");
        JoinRelation matchJoinRelation = getMatchJoinRelation(before, tableView, schema);
        SqlNode joinRelationCondition = null;
        if (!CollectionUtils.isEmpty(matchJoinRelation.getJoinCondition())) {
            sqlLiteral = SemanticNode.getJoinSqlLiteral(matchJoinRelation.getJoinType());
            joinRelationCondition = getCondition(matchJoinRelation, scope, engineType);
            condition = joinRelationCondition;
        }
        if (Materialization.TimePartType.ZIPPER.equals(leftTable.getDataSource().getTimePartType())
                || Materialization.TimePartType.ZIPPER.equals(tableView.getDataSource().getTimePartType())) {
            SqlNode zipperCondition = getZipperCondition(leftTable, tableView, dataSource, schema, scope);
            if (Objects.nonNull(joinRelationCondition)) {
                condition = new SqlBasicCall(
                        SqlStdOperatorTable.AND,
                        new ArrayList<>(Arrays.asList(zipperCondition, joinRelationCondition)),
                        SqlParserPos.ZERO, null);
            } else {
                condition = zipperCondition;
            }
        }

        return new SqlJoin(
                SqlParserPos.ZERO,
                left,
                SqlLiteral.createBoolean(false, SqlParserPos.ZERO),
                sqlLiteral,
                SemanticNode.buildAs(tableView.getAlias(), getTable(tableView, scope)),
                SqlLiteral.createSymbol(JoinConditionType.ON, SqlParserPos.ZERO),
                condition
        );
    }

    private JoinRelation getMatchJoinRelation(Map<String, String> before, TableView tableView, SemanticSchema schema) {
        JoinRelation matchJoinRelation = JoinRelation.builder().build();
        if (!CollectionUtils.isEmpty(schema.getJoinRelations())) {
            for (JoinRelation joinRelation : schema.getJoinRelations()) {
                if (joinRelation.getRight().equalsIgnoreCase(tableView.getDataSource().getName())
                        && before.containsKey(joinRelation.getLeft())) {
                    matchJoinRelation.setJoinCondition(joinRelation.getJoinCondition().stream()
                            .map(r -> Triple.of(before.get(joinRelation.getLeft()) + "." + r.getLeft(),
                                    r.getMiddle(), tableView.getAlias() + "." + r.getRight())).collect(
                                    Collectors.toList()));
                    matchJoinRelation.setJoinType(joinRelation.getJoinType());
                }
            }

        }
        return matchJoinRelation;
    }

    private SqlNode getCondition(JoinRelation joinRelation, SqlValidatorScope scope, EngineType engineType)
            throws Exception {
        SqlNode condition = null;
        for (Triple<String, String, String> con : joinRelation.getJoinCondition()) {
            List<SqlNode> ons = new ArrayList<>();
            ons.add(SemanticNode.parse(con.getLeft(), scope, engineType));
            ons.add(SemanticNode.parse(con.getRight(), scope, engineType));
            if (Objects.isNull(condition)) {
                condition = new SqlBasicCall(
                        SemanticNode.getBinaryOperator(con.getMiddle()),
                        ons,
                        SqlParserPos.ZERO, null);
                continue;
            }
            SqlNode addCondition = new SqlBasicCall(
                    SemanticNode.getBinaryOperator(con.getMiddle()),
                    ons,
                    SqlParserPos.ZERO, null);
            condition = new SqlBasicCall(
                    SqlStdOperatorTable.AND,
                    new ArrayList<>(Arrays.asList(condition, addCondition)),
                    SqlParserPos.ZERO, null);
        }
        return condition;
    }

    private SqlNode getCondition(TableView left, TableView right, DataSource dataSource, SemanticSchema schema,
            SqlValidatorScope scope, EngineType engineType) throws Exception {

        Set<String> selectLeft = SemanticNode.getSelect(left.getTable());
        Set<String> selectRight = SemanticNode.getSelect(right.getTable());
        selectLeft.retainAll(selectRight);
        SqlNode condition = null;
        for (String on : selectLeft) {
            if (!SourceRender.isDimension(on, dataSource, schema)) {
                continue;
            }
            if (IdentifyNode.isForeign(on, left.getDataSource().getIdentifiers())) {
                if (!IdentifyNode.isPrimary(on, right.getDataSource().getIdentifiers())) {
                    continue;
                }
            }
            if (IdentifyNode.isForeign(on, right.getDataSource().getIdentifiers())) {
                if (!IdentifyNode.isPrimary(on, left.getDataSource().getIdentifiers())) {
                    continue;
                }
            }
            List<SqlNode> ons = new ArrayList<>();
            ons.add(SemanticNode.parse(left.getAlias() + "." + on, scope, engineType));
            ons.add(SemanticNode.parse(right.getAlias() + "." + on, scope, engineType));
            if (condition == null) {
                condition = new SqlBasicCall(
                        SqlStdOperatorTable.EQUALS,
                        ons,
                        SqlParserPos.ZERO, null);
                continue;
            }
            SqlNode addCondition = new SqlBasicCall(
                    SqlStdOperatorTable.EQUALS,
                    ons,
                    SqlParserPos.ZERO, null);
            condition = new SqlBasicCall(
                    SqlStdOperatorTable.AND,
                    new ArrayList<>(Arrays.asList(condition, addCondition)),
                    SqlParserPos.ZERO, null);
        }
        return condition;
    }

    private static void joinOrder(int cnt, String id, Map<String, Set<String>> next, Queue<String> orders,
            Map<String, Boolean> visited) {
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

    private void addZipperField(DataSource dataSource, List<String> fields) {
        if (Materialization.TimePartType.ZIPPER.equals(dataSource.getTimePartType())) {
            dataSource.getDimensions().stream()
                    .filter(d -> Constants.DIMENSION_TYPE_TIME.equalsIgnoreCase(d.getType())).forEach(t -> {
                        if (t.getName().startsWith(Constants.MATERIALIZATION_ZIPPER_END)
                                && !fields.contains(t.getName())
                        ) {
                            fields.add(t.getName());
                        }
                        if (t.getName().startsWith(Constants.MATERIALIZATION_ZIPPER_START)
                                && !fields.contains(t.getName())
                        ) {
                            fields.add(t.getName());
                        }
                    });
        }
    }

    private SqlNode getZipperCondition(TableView left, TableView right, DataSource dataSource, SemanticSchema schema,
            SqlValidatorScope scope) throws Exception {
        if (Materialization.TimePartType.ZIPPER.equals(left.getDataSource().getTimePartType())
                && Materialization.TimePartType.ZIPPER.equals(right.getDataSource().getTimePartType())) {
            throw new Exception("not support two zipper table");
        }
        SqlNode condition = null;
        Optional<Dimension> leftTime = left.getDataSource().getDimensions().stream()
                .filter(d -> Constants.DIMENSION_TYPE_TIME.equalsIgnoreCase(d.getType())).findFirst();
        Optional<Dimension> rightTime = right.getDataSource().getDimensions().stream()
                .filter(d -> Constants.DIMENSION_TYPE_TIME.equalsIgnoreCase(d.getType())).findFirst();
        if (leftTime.isPresent() && rightTime.isPresent()) {

            String startTime = "";
            String endTime = "";
            String dateTime = "";

            Optional<Dimension> startTimeOp =
                    (Materialization.TimePartType.ZIPPER.equals(left.getDataSource().getTimePartType()) ? left : right)
                            .getDataSource().getDimensions().stream()
                            .filter(d -> Constants.DIMENSION_TYPE_TIME.equalsIgnoreCase(d.getType()))
                            .filter(d -> d.getName().startsWith(Constants.MATERIALIZATION_ZIPPER_START)).findFirst();
            Optional<Dimension> endTimeOp =
                    (Materialization.TimePartType.ZIPPER.equals(left.getDataSource().getTimePartType()) ? left : right)
                            .getDataSource().getDimensions().stream()
                            .filter(d -> Constants.DIMENSION_TYPE_TIME.equalsIgnoreCase(d.getType()))
                            .filter(d -> d.getName().startsWith(Constants.MATERIALIZATION_ZIPPER_END)).findFirst();
            if (startTimeOp.isPresent() && endTimeOp.isPresent()) {
                TableView zipper =
                        Materialization.TimePartType.ZIPPER.equals(left.getDataSource().getTimePartType())
                                ? left : right;
                TableView partMetric =
                        Materialization.TimePartType.ZIPPER.equals(left.getDataSource().getTimePartType())
                                ? right : left;
                Optional<Dimension> partTime =
                        Materialization.TimePartType.ZIPPER.equals(left.getDataSource().getTimePartType())
                                ? rightTime : leftTime;
                startTime = zipper.getAlias() + "." + startTimeOp.get().getName();
                endTime = zipper.getAlias() + "." + endTimeOp.get().getName();
                dateTime = partMetric.getAlias() + "." + partTime.get().getName();
            }
            EngineType engineType = EngineType.fromString(schema.getSemanticModel().getDatabase().getType());
            ArrayList<SqlNode> operandList = new ArrayList<>(
                    Arrays.asList(SemanticNode.parse(endTime, scope, engineType),
                            SemanticNode.parse(dateTime, scope, engineType)));
            condition =
                    new SqlBasicCall(
                            SqlStdOperatorTable.AND,
                            new ArrayList<SqlNode>(Arrays.asList(new SqlBasicCall(
                                    SqlStdOperatorTable.LESS_THAN_OR_EQUAL,
                                    new ArrayList<SqlNode>(
                                            Arrays.asList(SemanticNode.parse(startTime, scope, engineType),
                                                    SemanticNode.parse(dateTime, scope, engineType))),
                                    SqlParserPos.ZERO, null), new SqlBasicCall(
                                    SqlStdOperatorTable.GREATER_THAN,
                                    operandList,
                                    SqlParserPos.ZERO, null))),
                            SqlParserPos.ZERO, null);

        }
        return condition;
    }
}
