package com.tencent.supersonic.semantic.query.parser.calcite.sql.node;


import com.tencent.supersonic.semantic.api.query.request.MetricReq;
import com.tencent.supersonic.semantic.query.parser.calcite.Configuration;
import com.tencent.supersonic.semantic.query.parser.calcite.s2ql.Constants;
import com.tencent.supersonic.semantic.query.parser.calcite.s2ql.DataSource;
import com.tencent.supersonic.semantic.query.parser.calcite.s2ql.Dimension;
import com.tencent.supersonic.semantic.query.parser.calcite.schema.SemanticSchema;
import com.tencent.supersonic.semantic.query.parser.calcite.sql.node.extend.LateralViewExplodeNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlDataTypeSpec;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlUserDefinedTypeNameSpec;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.validate.SqlValidatorScope;
import org.springframework.util.CollectionUtils;

@Slf4j
public class DataSourceNode extends SemanticNode {

    public static SqlNode build(DataSource datasource, SqlValidatorScope scope) throws Exception {
        String sqlTable = "";
        if (datasource.getSqlQuery() != null && !datasource.getSqlQuery().isEmpty()) {
            sqlTable = datasource.getSqlQuery();
        } else if (datasource.getTableQuery() != null && !datasource.getTableQuery().isEmpty()) {
            sqlTable = "select * from " + datasource.getTableQuery();
        }
        if (sqlTable.isEmpty()) {
            throw new Exception("DatasourceNode build error [tableSqlNode not found]");
        }
        return buildAs(datasource.getName(), getTable(sqlTable, scope));
    }


    public static SqlNode buildExtend(DataSource datasource, Set<String> exprList,
            SqlValidatorScope scope)
            throws Exception {
        if (CollectionUtils.isEmpty(exprList)) {
            return build(datasource, scope);
        }
        SqlNode view = new SqlBasicCall(new LateralViewExplodeNode(), Arrays.asList(build(datasource, scope),
                new SqlNodeList(getExtendField(exprList, scope), SqlParserPos.ZERO)), SqlParserPos.ZERO);
        return buildAs(datasource.getName() + Constants.DIMENSION_ARRAY_SINGLE_SUFFIX, view);
    }

    public static List<SqlNode> getExtendField(Set<String> exprList, SqlValidatorScope scope) throws Exception {
        List<SqlNode> sqlNodeList = new ArrayList<>();
        for (String expr : exprList) {
            sqlNodeList.add(parse(expr, scope));
            sqlNodeList.add(new SqlDataTypeSpec(
                    new SqlUserDefinedTypeNameSpec(expr + Constants.DIMENSION_ARRAY_SINGLE_SUFFIX, SqlParserPos.ZERO),
                    SqlParserPos.ZERO));
        }
        return sqlNodeList;
    }

    private static SqlNode getTable(String sqlQuery, SqlValidatorScope scope) throws Exception {
        SqlParser sqlParser = SqlParser.create(sqlQuery, Configuration.getParserConfig());
        SqlNode sqlNode = sqlParser.parseQuery();
        scope.validateExpr(sqlNode);
        return sqlNode;
    }

    public static String getNames(List<DataSource> dataSourceList) {
        return dataSourceList.stream().map(d -> d.getName()).collect(Collectors.joining("_"));
    }

    public static void getQueryDimensionMeasure(SemanticSchema schema, MetricReq metricCommand,
            Set<String> queryDimension, List<String> measures) {
        queryDimension.addAll(metricCommand.getDimensions().stream()
                .map(d -> d.contains(Constants.DIMENSION_IDENTIFY) ? d.split(Constants.DIMENSION_IDENTIFY)[1] : d)
                .collect(Collectors.toSet()));
        Set<String> schemaMetricName = schema.getMetrics().stream().map(m -> m.getName()).collect(Collectors.toSet());
        schema.getMetrics().stream().filter(m -> metricCommand.getMetrics().contains(m.getName()))
                .forEach(m -> m.getMetricTypeParams().getMeasures().stream().forEach(mm -> measures.add(mm.getName())));
        metricCommand.getMetrics().stream().filter(m -> !schemaMetricName.contains(m)).forEach(m -> measures.add(m));

    }

    public static void mergeQueryFilterDimensionMeasure(SemanticSchema schema, MetricReq metricCommand,
            Set<String> queryDimension, List<String> measures,
            SqlValidatorScope scope) throws Exception {
        if (Objects.nonNull(metricCommand.getWhere()) && !metricCommand.getWhere().isEmpty()) {
            Set<String> filterConditions = new HashSet<>();
            FilterNode.getFilterField(parse(metricCommand.getWhere(), scope), filterConditions);
            Set<String> queryMeasures = new HashSet<>(measures);
            Set<String> schemaMetricName = schema.getMetrics().stream()
                    .map(m -> m.getName()).collect(Collectors.toSet());
            for (String filterCondition : filterConditions) {
                if (schemaMetricName.contains(filterCondition)) {
                    schema.getMetrics().stream().filter(m -> m.getName().equalsIgnoreCase(filterCondition))
                            .forEach(m -> m.getMetricTypeParams().getMeasures().stream()
                                    .forEach(mm -> queryMeasures.add(mm.getName())));
                    continue;
                }
                queryDimension.add(filterCondition);
            }
            measures.clear();
            measures.addAll(queryMeasures);
        }
    }

    public static List<DataSource> getMatchDataSources(SqlValidatorScope scope, SemanticSchema schema,
            MetricReq metricCommand) throws Exception {
        List<DataSource> dataSources = new ArrayList<>();

        // check by metric
        List<String> measures = new ArrayList<>();
        Set<String> queryDimension = new HashSet<>();
        getQueryDimensionMeasure(schema, metricCommand, queryDimension, measures);
        DataSource baseDataSource = null;
        // one , match measure count
        Map<String, Integer> dataSourceMeasures = new HashMap<>();
        for (Map.Entry<String, DataSource> entry : schema.getDatasource().entrySet()) {
            Set<String> sourceMeasure = entry.getValue().getMeasures().stream().map(mm -> mm.getName())
                    .collect(Collectors.toSet());
            sourceMeasure.retainAll(measures);
            dataSourceMeasures.put(entry.getKey(), sourceMeasure.size());
        }
        log.info("dataSourceMeasures [{}]", dataSourceMeasures);
        Optional<Map.Entry<String, Integer>> base = dataSourceMeasures.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).findFirst();
        if (base.isPresent()) {
            baseDataSource = schema.getDatasource().get(base.get().getKey());
            dataSources.add(baseDataSource);
        }
        // second , check match all dimension and metric
        if (baseDataSource != null) {
            Set<String> filterMeasure = new HashSet<>();
            Set<String> sourceMeasure = baseDataSource.getMeasures().stream().map(mm -> mm.getName())
                    .collect(Collectors.toSet());
            Set<String> dimension = baseDataSource.getDimensions().stream().map(dd -> dd.getName())
                    .collect(Collectors.toSet());
            baseDataSource.getIdentifiers().stream().forEach(i -> dimension.add(i.getName()));
            if (schema.getDimension().containsKey(baseDataSource.getName())) {
                schema.getDimension().get(baseDataSource.getName()).stream().forEach(d -> dimension.add(d.getName()));
            }
            filterMeasure.addAll(sourceMeasure);
            filterMeasure.addAll(dimension);
            mergeQueryFilterDimensionMeasure(schema, metricCommand, queryDimension, measures, scope);
            boolean isAllMatch = checkMatch(sourceMeasure, queryDimension, measures, dimension, metricCommand, scope);
            if (isAllMatch) {
                log.info("baseDataSource  match all ");
                return dataSources;
            }
            // find all dataSource has the same identifiers
            Set<String> baseIdentifiers = baseDataSource.getIdentifiers().stream().map(i -> i.getName())
                    .collect(Collectors.toSet());
            if (baseIdentifiers.isEmpty()) {
                throw new Exception("datasource error : " + baseDataSource.getName() + " miss identifier");
            }
            List<DataSource> linkDataSources = getLinkDataSources(baseIdentifiers, queryDimension, measures,
                    baseDataSource, schema);
            if (linkDataSources.isEmpty()) {
                throw new Exception(
                        String.format("not find the match datasource : dimension[%s],measure[%s]", queryDimension,
                                measures));
            }
            log.debug("linkDataSources {}", linkDataSources);

            dataSources.addAll(linkDataSources);
        }

        return dataSources;
    }

    private static boolean checkMatch(Set<String> sourceMeasure,
            Set<String> queryDimension,
            List<String> measures,
            Set<String> dimension,
            MetricReq metricCommand,
            SqlValidatorScope scope) throws Exception {
        boolean isAllMatch = true;
        sourceMeasure.retainAll(measures);
        if (sourceMeasure.size() < measures.size()) {
            log.info("baseDataSource not match all measure");
            isAllMatch = false;
        }
        measures.removeAll(sourceMeasure);

        dimension.retainAll(queryDimension);
        if (dimension.size() < queryDimension.size()) {
            log.info("baseDataSource not match all dimension");
            isAllMatch = false;
        }
        queryDimension.removeAll(dimension);

        if (metricCommand.getWhere() != null && !metricCommand.getWhere().isEmpty()) {
            Set<String> whereFields = new HashSet<>();
            SqlNode sqlNode = parse(metricCommand.getWhere(), scope);
            FilterNode.getFilterField(sqlNode, whereFields);
        }
        return isAllMatch;
    }

    private static List<DataSource> getLinkDataSources(Set<String> baseIdentifiers,
            Set<String> queryDimension,
            List<String> measures,
            DataSource baseDataSource,
            SemanticSchema schema) {
        Set<String> linkDataSourceName = new HashSet<>();
        List<DataSource> linkDataSources = new ArrayList<>();
        for (Map.Entry<String, DataSource> entry : schema.getDatasource().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(baseDataSource.getName())) {
                continue;
            }
            Long identifierNum = entry.getValue().getIdentifiers().stream().map(i -> i.getName())
                    .filter(i -> baseIdentifiers.contains(i)).count();
            if (identifierNum > 0) {
                boolean isMatch = false;
                if (!queryDimension.isEmpty()) {
                    Set<String> linkDimension = entry.getValue().getDimensions().stream().map(dd -> dd.getName())
                            .collect(Collectors.toSet());
                    entry.getValue().getIdentifiers().stream().forEach(i -> linkDimension.add(i.getName()));
                    linkDimension.retainAll(queryDimension);
                    if (!linkDimension.isEmpty()) {
                        isMatch = true;
                    }
                }
                if (!measures.isEmpty()) {
                    Set<String> linkMeasure = entry.getValue().getMeasures().stream().map(mm -> mm.getName())
                            .collect(Collectors.toSet());
                    linkMeasure.retainAll(measures);
                    if (!linkMeasure.isEmpty()) {
                        isMatch = true;
                    }
                }
                if (isMatch) {
                    linkDataSourceName.add(entry.getKey());
                }
            }
        }
        for (Map.Entry<String, List<Dimension>> entry : schema.getDimension().entrySet()) {
            if (!queryDimension.isEmpty()) {
                Set<String> linkDimension = entry.getValue().stream().map(dd -> dd.getName())
                        .collect(Collectors.toSet());
                linkDimension.retainAll(queryDimension);
                if (!linkDimension.isEmpty()) {
                    linkDataSourceName.add(entry.getKey());
                }
            }
        }
        for (String linkName : linkDataSourceName) {
            linkDataSources.add(schema.getDatasource().get(linkName));
        }
        return linkDataSources;
    }
}
