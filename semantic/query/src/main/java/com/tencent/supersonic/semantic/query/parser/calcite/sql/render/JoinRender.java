package com.tencent.supersonic.semantic.query.parser.calcite.sql.render;

import com.tencent.supersonic.semantic.api.query.request.MetricReq;
import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.Constants;
import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.DataSource;
import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.Dimension;
import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.Identify;

import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.Identify.Type;
import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.Materialization.TimePartType;

import com.tencent.supersonic.semantic.query.parser.calcite.s2sql.Metric;
import com.tencent.supersonic.semantic.query.parser.calcite.schema.SemanticSchema;
import com.tencent.supersonic.semantic.query.parser.calcite.sql.Renderer;
import com.tencent.supersonic.semantic.query.parser.calcite.sql.TableView;
import com.tencent.supersonic.semantic.query.parser.calcite.sql.node.AggFunctionNode;
import com.tencent.supersonic.semantic.query.parser.calcite.sql.node.DataSourceNode;
import com.tencent.supersonic.semantic.query.parser.calcite.sql.node.FilterNode;
import com.tencent.supersonic.semantic.query.parser.calcite.sql.node.IdentifyNode;
import com.tencent.supersonic.semantic.query.parser.calcite.sql.node.MetricNode;
import com.tencent.supersonic.semantic.query.parser.calcite.sql.node.SemanticNode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.JoinConditionType;
import org.apache.calcite.sql.JoinType;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.validate.SqlValidatorScope;
import org.springframework.util.CollectionUtils;

@Slf4j
public class JoinRender extends Renderer {

    @Override
    public void render(MetricReq metricCommand, List<DataSource> dataSources, SqlValidatorScope scope,
            SemanticSchema schema, boolean nonAgg) throws Exception {
        String queryWhere = metricCommand.getWhere();
        dataSources = getOrderSource(dataSources);
        Set<String> whereFields = new HashSet<>();
        List<String> fieldWhere = new ArrayList<>();
        if (queryWhere != null && !queryWhere.isEmpty()) {
            SqlNode sqlNode = SemanticNode.parse(queryWhere, scope);
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
                continue;
            }

            left = new SqlJoin(
                    SqlParserPos.ZERO,
                    left,
                    SqlLiteral.createBoolean(false, SqlParserPos.ZERO),
                    SqlLiteral.createSymbol(JoinType.INNER, SqlParserPos.ZERO),
                    SemanticNode.buildAs(tableView.getAlias(), getTable(tableView, scope)),
                    SqlLiteral.createSymbol(JoinConditionType.ON, SqlParserPos.ZERO),
                    getCondition(leftTable, tableView, dataSource, schema, scope));
            leftTable = tableView;
        }

        for (Map.Entry<String, SqlNode> entry : innerSelect.entrySet()) {
            innerView.getMeasure().add(entry.getValue());
        }
        innerView.setTable(left);
        filterView.setTable(SemanticNode.buildAs(Constants.JOIN_TABLE_OUT_PREFIX, innerView.build()));
        if (!filterDimension.isEmpty()) {
            for (String d : getQueryDimension(filterDimension, queryAllDimension, whereFields)) {
                if (nonAgg) {
                    filterView.getMeasure().add(SemanticNode.parse(d, scope));
                } else {
                    filterView.getDimension().add(SemanticNode.parse(d, scope));
                }

            }
        }
        super.tableView = filterView;
    }

    private void doMetric(Map<String, SqlNode> innerSelect, TableView filterView, List<String> queryMetrics,
            List<String> reqMetrics, DataSource dataSource, Set<String> sourceMeasure, SqlValidatorScope scope,
            SemanticSchema schema, boolean nonAgg) throws Exception {
        String alias = Constants.JOIN_TABLE_PREFIX + dataSource.getName();
        for (String m : reqMetrics) {
            if (getMatchMetric(schema, sourceMeasure, m, queryMetrics)) {
                MetricNode metricNode = buildMetricNode(m, dataSource, scope, schema, nonAgg, alias);

                if (!metricNode.getNonAggNode().isEmpty()) {
                    for (String measure : metricNode.getNonAggNode().keySet()) {
                        innerSelect.put(measure,
                                SemanticNode.buildAs(measure, SemanticNode.parse(alias + "." + measure, scope)));
                    }

                }
                if (metricNode.getAggFunction() != null && !metricNode.getAggFunction().isEmpty()) {
                    for (Map.Entry<String, String> entry : metricNode.getAggFunction().entrySet()) {
                        if (metricNode.getNonAggNode().containsKey(entry.getKey())) {
                            if (nonAgg) {
                                filterView.getMeasure().add(SemanticNode.buildAs(entry.getKey(),
                                        SemanticNode.parse(entry.getKey(), scope)));
                            } else {
                                filterView.getMeasure().add(SemanticNode.buildAs(entry.getKey(),
                                        AggFunctionNode.build(entry.getValue(), entry.getKey(), scope)));
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
        for (String d : reqDimensions) {
            if (getMatchDimension(schema, dimension, dataSource, d, queryDimension)) {
                if (d.contains(Constants.DIMENSION_IDENTIFY)) {
                    String[] identifyDimension = d.split(Constants.DIMENSION_IDENTIFY);
                    innerSelect.put(d,
                            SemanticNode.buildAs(d, SemanticNode.parse(alias + "." + identifyDimension[1], scope)));
                } else {
                    innerSelect.put(d, SemanticNode.buildAs(d, SemanticNode.parse(alias + "." + d, scope)));

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

    private SqlNode getCondition(TableView left, TableView right, DataSource dataSource, SemanticSchema schema,
            SqlValidatorScope scope) throws Exception {
        if (TimePartType.ZIPPER.equals(left.getDataSource().getTimePartType()) || TimePartType.ZIPPER.equals(
                right.getDataSource().getTimePartType())) {
            return getZipperCondition(left, right, dataSource, schema, scope);
        }
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
            ons.add(SemanticNode.parse(left.getAlias() + "." + on, scope));
            ons.add(SemanticNode.parse(right.getAlias() + "." + on, scope));
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

    private List<DataSource> getOrderSource(List<DataSource> dataSources) throws Exception {
        if (CollectionUtils.isEmpty(dataSources) || dataSources.size() <= 2) {
            return dataSources;
        }
        Map<String, Set<String>> next = new HashMap<>();
        Map<String, Boolean> visited = new HashMap<>();
        Map<String, List<Identify>> dataSourceIdentifies = new HashMap<>();
        dataSources.stream().forEach(d -> {
            next.put(d.getName(), new HashSet<>());
            visited.put(d.getName(), false);
            dataSourceIdentifies.put(d.getName(), d.getIdentifiers());
        });
        int cnt = dataSources.size();
        List<Map.Entry<String, List<Identify>>> dataSourceIdentifyList = dataSourceIdentifies.entrySet().stream()
                .collect(
                        Collectors.toList());
        for (int i = 0; i < cnt; i++) {
            for (int j = i + 1; j < cnt; j++) {
                Set<String> primaries = IdentifyNode.getIdentifyNames(dataSourceIdentifyList.get(i).getValue(),
                        Type.PRIMARY);
                Set<String> foreign = IdentifyNode.getIdentifyNames(dataSourceIdentifyList.get(i).getValue(),
                        Type.FOREIGN);
                Set<String> nextPrimaries = IdentifyNode.getIdentifyNames(dataSourceIdentifyList.get(j).getValue(),
                        Type.PRIMARY);
                Set<String> nextForeign = IdentifyNode.getIdentifyNames(dataSourceIdentifyList.get(j).getValue(),
                        Type.FOREIGN);
                Set<String> nextAll = new HashSet<>();
                nextAll.addAll(nextPrimaries);
                nextAll.addAll(nextForeign);
                primaries.retainAll(nextPrimaries);
                foreign.retainAll(nextPrimaries);
                if (primaries.size() > 0 || foreign.size() > 0) {
                    next.get(dataSourceIdentifyList.get(i).getKey()).add(dataSourceIdentifyList.get(j).getKey());
                    next.get(dataSourceIdentifyList.get(j).getKey()).add(dataSourceIdentifyList.get(i).getKey());
                }

            }
        }
        Queue<String> paths = new ArrayDeque<>();
        for (String id : visited.keySet()) {
            if (!visited.get(id)) {
                joinOrder(cnt, id, next, paths, visited);
                if (paths.size() >= cnt) {
                    break;
                }
            }
        }
        if (paths.size() < cnt) {
            throw new Exception("datasource cant join,pls check identify :" + dataSources.stream()
                    .map(d -> d.getName()).collect(
                            Collectors.joining(",")));
        }
        List<String> orderList = new ArrayList<>(paths);
        Collections.sort(dataSources, new Comparator<DataSource>() {
            @Override
            public int compare(DataSource o1, DataSource o2) {
                return orderList.indexOf(o1.getName()) - orderList.indexOf(o2.getName());
            }
        });
        return dataSources;
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
        if (TimePartType.ZIPPER.equals(dataSource.getTimePartType())) {
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
        if (TimePartType.ZIPPER.equals(left.getDataSource().getTimePartType()) && TimePartType.ZIPPER.equals(
                right.getDataSource().getTimePartType())) {
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
            List<String> primaryZipper = new ArrayList<>();
            List<String> primaryPartition = new ArrayList<>();

            Optional<Dimension> startTimeOp = (TimePartType.ZIPPER.equals(left.getDataSource().getTimePartType()) ? left
                    : right).getDataSource().getDimensions().stream()
                    .filter(d -> Constants.DIMENSION_TYPE_TIME.equalsIgnoreCase(d.getType()))
                    .filter(d -> d.getName().startsWith(Constants.MATERIALIZATION_ZIPPER_START)).findFirst();
            Optional<Dimension> endTimeOp = (TimePartType.ZIPPER.equals(left.getDataSource().getTimePartType()) ? left
                    : right).getDataSource().getDimensions().stream()
                    .filter(d -> Constants.DIMENSION_TYPE_TIME.equalsIgnoreCase(d.getType()))
                    .filter(d -> d.getName().startsWith(Constants.MATERIALIZATION_ZIPPER_END)).findFirst();
            if (startTimeOp.isPresent() && endTimeOp.isPresent()) {
                TableView zipper = TimePartType.ZIPPER.equals(left.getDataSource().getTimePartType()) ? left : right;
                TableView partMetric =
                        TimePartType.ZIPPER.equals(left.getDataSource().getTimePartType()) ? right : left;
                Optional<Dimension> partTime =
                        TimePartType.ZIPPER.equals(left.getDataSource().getTimePartType()) ? rightTime : leftTime;
                startTime = zipper.getAlias() + "." + startTimeOp.get().getName();
                endTime = zipper.getAlias() + "." + endTimeOp.get().getName();
                dateTime = partMetric.getAlias() + "." + partTime.get().getName();
                primaryZipper = zipper.getDataSource().getIdentifiers().stream().map(i -> i.getName()).collect(
                        Collectors.toList());
                primaryPartition = partMetric.getDataSource().getIdentifiers().stream().map(i -> i.getName()).collect(
                        Collectors.toList());
            }
            primaryZipper.retainAll(primaryPartition);
            condition =
                    new SqlBasicCall(
                            SqlStdOperatorTable.AND,
                            new ArrayList<SqlNode>(Arrays.asList(new SqlBasicCall(
                                    SqlStdOperatorTable.LESS_THAN_OR_EQUAL,
                                    new ArrayList<SqlNode>(Arrays.asList(SemanticNode.parse(startTime, scope),
                                            SemanticNode.parse(dateTime, scope))),
                                    SqlParserPos.ZERO, null), new SqlBasicCall(
                                    SqlStdOperatorTable.GREATER_THAN,
                                            new ArrayList<SqlNode>(Arrays.asList(SemanticNode.parse(endTime, scope),
                                            SemanticNode.parse(dateTime, scope))),
                                    SqlParserPos.ZERO, null))),
                            SqlParserPos.ZERO, null);

            for (String p : primaryZipper) {
                List<SqlNode> ons = new ArrayList<>();
                ons.add(SemanticNode.parse(left.getAlias() + "." + p, scope));
                ons.add(SemanticNode.parse(right.getAlias() + "." + p, scope));
                SqlNode addCondition = new SqlBasicCall(
                        SqlStdOperatorTable.EQUALS,
                        ons,
                        SqlParserPos.ZERO, null);
                condition = new SqlBasicCall(
                        SqlStdOperatorTable.AND,
                        new ArrayList<>(Arrays.asList(condition, addCondition)),
                        SqlParserPos.ZERO, null);
            }

        }
        return condition;
    }
}
