package com.tencent.supersonic.headless.core.translator;

import com.tencent.supersonic.common.calcite.SqlMergeWithUtils;
import com.tencent.supersonic.common.jsqlparser.SqlRemoveHelper;
import com.tencent.supersonic.common.jsqlparser.SqlReplaceHelper;
import com.tencent.supersonic.common.jsqlparser.SqlSelectFunctionHelper;
import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.StringUtil;
import com.tencent.supersonic.headless.api.pojo.Measure;
import com.tencent.supersonic.headless.api.pojo.MetricTable;
import com.tencent.supersonic.headless.api.pojo.QueryParam;
import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import com.tencent.supersonic.headless.api.pojo.enums.MetricType;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.core.adaptor.db.DbAdaptor;
import com.tencent.supersonic.headless.core.adaptor.db.DbAdaptorFactory;
import com.tencent.supersonic.headless.core.pojo.DataSetQueryParam;
import com.tencent.supersonic.headless.core.pojo.MetricQueryParam;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.translator.calcite.s2sql.Ontology;
import com.tencent.supersonic.headless.core.translator.converter.QueryConverter;
import com.tencent.supersonic.headless.core.utils.ComponentFactory;
import com.tencent.supersonic.headless.core.utils.SqlGenerateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
public class DefaultSemanticTranslator implements SemanticTranslator {

    @Autowired
    private SqlGenerateUtils sqlGenerateUtils;

    public void translate(QueryStatement queryStatement) {
        if (queryStatement.isTranslated()) {
            return;
        }

        try {
            preprocess(queryStatement);
            parse(queryStatement);
            optimize(queryStatement);
        } catch (Exception e) {
            queryStatement.setErrMsg(e.getMessage());
        }
    }

    private void parse(QueryStatement queryStatement) throws Exception {
        QueryParam queryParam = queryStatement.getQueryParam();
        if (Objects.isNull(queryStatement.getDataSetQueryParam())) {
            queryStatement.setDataSetQueryParam(new DataSetQueryParam());
        }
        if (Objects.isNull(queryStatement.getMetricQueryParam())) {
            queryStatement.setMetricQueryParam(new MetricQueryParam());
        }

        log.debug("SemanticConverter before [{}]", queryParam);
        for (QueryConverter headlessConverter : ComponentFactory.getQueryConverters()) {
            if (headlessConverter.accept(queryStatement)) {
                log.debug("SemanticConverter accept [{}]", headlessConverter.getClass().getName());
                headlessConverter.convert(queryStatement);
            }
        }
        log.debug("SemanticConverter after {} {} {}", queryParam,
                queryStatement.getDataSetQueryParam(), queryStatement.getMetricQueryParam());

        if (!queryStatement.getDataSetQueryParam().getSql().isEmpty()) {
            doParse(queryStatement.getDataSetQueryParam(), queryStatement);
        } else {
            queryStatement.getMetricQueryParam()
                    .setNativeQuery(queryParam.getQueryType().isNativeAggQuery());
            doParse(queryStatement,
                    AggOption.getAggregation(queryStatement.getMetricQueryParam().isNativeQuery()));
        }

        if (StringUtils.isEmpty(queryStatement.getSql())) {
            throw new RuntimeException("parse Exception: " + queryStatement.getErrMsg());
        }
        if (StringUtils.isNotBlank(queryStatement.getSql())
                && !SqlSelectHelper.hasLimit(queryStatement.getSql())) {
            String querySql =
                    queryStatement.getSql() + " limit " + queryStatement.getLimit().toString();
            queryStatement.setSql(querySql);
        }
    }

    private QueryStatement doParse(DataSetQueryParam dataSetQueryParam,
            QueryStatement queryStatement) {
        log.info("parse dataSetQuery [{}] ", dataSetQueryParam);
        Ontology ontology = queryStatement.getOntology();
        EngineType engineType = EngineType.fromString(ontology.getDatabase().getType());
        try {
            if (!CollectionUtils.isEmpty(dataSetQueryParam.getTables())) {
                List<String[]> tables = new ArrayList<>();
                boolean isSingleTable = dataSetQueryParam.getTables().size() == 1;
                for (MetricTable metricTable : dataSetQueryParam.getTables()) {
                    QueryStatement tableSql = parserSql(metricTable, isSingleTable,
                            dataSetQueryParam, queryStatement);
                    if (isSingleTable && StringUtils.isNotBlank(tableSql.getDataSetSimplifySql())) {
                        queryStatement.setSql(tableSql.getDataSetSimplifySql());
                        queryStatement.setDataSetQueryParam(dataSetQueryParam);
                        return queryStatement;
                    }
                    tables.add(new String[] {metricTable.getAlias(), tableSql.getSql()});
                }
                if (!tables.isEmpty()) {
                    String sql;
                    if (dataSetQueryParam.isSupportWith()) {
                        if (!SqlMergeWithUtils.hasWith(engineType, dataSetQueryParam.getSql())) {
                            sql = "with "
                                    + tables.stream()
                                            .map(t -> String.format("%s as (%s)", t[0], t[1]))
                                            .collect(Collectors.joining(","))
                                    + "\n" + dataSetQueryParam.getSql();
                        } else {
                            List<String> parentWithNameList = tables.stream().map(table -> table[0])
                                    .collect(Collectors.toList());
                            List<String> parentSqlList = tables.stream().map(table -> table[1])
                                    .collect(Collectors.toList());
                            sql = SqlMergeWithUtils.mergeWith(engineType,
                                    dataSetQueryParam.getSql(), parentSqlList, parentWithNameList);
                        }
                    } else {
                        sql = dataSetQueryParam.getSql();
                        for (String[] tb : tables) {
                            sql = StringUtils.replace(sql, tb[0], "(" + tb[1] + ") "
                                    + (dataSetQueryParam.isWithAlias() ? "" : tb[0]), -1);
                        }
                    }
                    queryStatement.setSql(sql);
                    queryStatement.setDataSetQueryParam(dataSetQueryParam);
                    return queryStatement;
                }
            }
        } catch (Exception e) {
            log.error("physicalSql error {}", e);
            queryStatement.setErrMsg(e.getMessage());
        }
        return queryStatement;
    }

    private QueryStatement doParse(QueryStatement queryStatement, AggOption isAgg) {
        MetricQueryParam metricQueryParam = queryStatement.getMetricQueryParam();
        log.info("parse metricQuery [{}] isAgg [{}]", metricQueryParam, isAgg);
        try {
            ComponentFactory.getQueryParser().parse(queryStatement, isAgg);
        } catch (Exception e) {
            queryStatement.setErrMsg(e.getMessage());
            log.error("parser error metricQueryReq[{}] error [{}]", metricQueryParam, e);
        }
        return queryStatement;
    }

    private QueryStatement parserSql(MetricTable metricTable, Boolean isSingleMetricTable,
            DataSetQueryParam dataSetQueryParam, QueryStatement queryStatement) throws Exception {
        MetricQueryParam metricQueryParam = new MetricQueryParam();
        metricQueryParam.setMetrics(metricTable.getMetrics());
        metricQueryParam.setDimensions(metricTable.getDimensions());
        metricQueryParam.setWhere(StringUtil.formatSqlQuota(metricTable.getWhere()));
        metricQueryParam.setNativeQuery(!AggOption.isAgg(metricTable.getAggOption()));

        QueryStatement tableSql = new QueryStatement();
        tableSql.setIsS2SQL(false);
        tableSql.setMetricQueryParam(metricQueryParam);
        tableSql.setMinMaxTime(queryStatement.getMinMaxTime());
        tableSql.setEnableOptimize(queryStatement.getEnableOptimize());
        tableSql.setDataSetId(queryStatement.getDataSetId());
        tableSql.setOntology(queryStatement.getOntology());
        if (isSingleMetricTable) {
            tableSql.setDataSetSql(dataSetQueryParam.getSql());
            tableSql.setDataSetAlias(metricTable.getAlias());
        }
        tableSql = doParse(tableSql, metricTable.getAggOption());
        if (!tableSql.isOk()) {
            throw new Exception(String.format("parser table [%s] error [%s]",
                    metricTable.getAlias(), tableSql.getErrMsg()));
        }
        return tableSql;
    }

    private void optimize(QueryStatement queryStatement) {
        for (QueryOptimizer queryOptimizer : ComponentFactory.getQueryOptimizers()) {
            queryOptimizer.rewrite(queryStatement);
        }
    }

    private void preprocess(QueryStatement queryStatement) {
        if (StringUtils.isBlank(queryStatement.getSql())) {
            return;
        }
        SemanticSchemaResp semanticSchemaResp = queryStatement.getSemanticSchemaResp();

        convertNameToBizName(queryStatement);
        rewriteFunction(queryStatement);
        queryStatement.setSql(SqlRemoveHelper.removeUnderscores(queryStatement.getSql()));

        String tableName = SqlSelectHelper.getTableName(queryStatement.getSql());
        if (StringUtils.isEmpty(tableName)) {
            return;
        }
        // correct order item is same as agg alias
        String reqSql = queryStatement.getSql();
        queryStatement.setSql(SqlReplaceHelper.replaceAggAliasOrderItem(queryStatement.getSql()));
        log.debug("replaceOrderAggSameAlias {} -> {}", reqSql, queryStatement.getSql());
        // 5.build MetricTables
        List<String> allFields = SqlSelectHelper.getAllSelectFields(queryStatement.getSql());
        List<MetricSchemaResp> metricSchemas = getMetrics(semanticSchemaResp, allFields);
        List<String> metrics =
                metricSchemas.stream().map(SchemaItem::getBizName).collect(Collectors.toList());
        Set<String> dimensions = getDimensions(semanticSchemaResp, allFields);
        QueryStructReq queryStructReq = new QueryStructReq();

        MetricTable metricTable = new MetricTable();
        metricTable.getMetrics().addAll(metrics);
        metricTable.getDimensions().addAll(dimensions);
        metricTable.setAlias(tableName.toLowerCase());
        // if metric empty , fill model default
        if (CollectionUtils.isEmpty(metricTable.getMetrics())) {
            metricTable.getMetrics().add(sqlGenerateUtils.generateInternalMetricName(
                    getDefaultModel(semanticSchemaResp, metricTable.getDimensions())));
        } else {
            queryStructReq.getAggregators()
                    .addAll(metricTable.getMetrics().stream()
                            .map(m -> new Aggregator(m, AggOperatorEnum.UNKNOWN))
                            .collect(Collectors.toList()));
        }
        AggOption aggOption = getAggOption(queryStatement, metricSchemas);
        metricTable.setAggOption(aggOption);
        List<MetricTable> tables = new ArrayList<>();
        tables.add(metricTable);

        // 6.build ParseSqlReq
        DataSetQueryParam datasetQueryParam = new DataSetQueryParam();
        datasetQueryParam.setTables(tables);
        datasetQueryParam.setSql(queryStatement.getSql());
        DatabaseResp database = semanticSchemaResp.getDatabaseResp();
        if (!sqlGenerateUtils.isSupportWith(EngineType.fromString(database.getType().toUpperCase()),
                database.getVersion())) {
            datasetQueryParam.setSupportWith(false);
            datasetQueryParam.setWithAlias(false);
        }

        // 7. do deriveMetric
        generateDerivedMetric(semanticSchemaResp, aggOption, datasetQueryParam);

        // 8.physicalSql by ParseSqlReq
        // queryStructReq.setDateInfo(queryStructUtils.getDateConfBySql(queryStatement.getSql()));
        queryStructReq.setDataSetId(queryStatement.getDataSetId());
        queryStructReq.setQueryType(getQueryType(aggOption));
        log.debug("QueryReqConverter queryStructReq[{}]", queryStructReq);
        QueryParam queryParam = new QueryParam();
        BeanUtils.copyProperties(queryStructReq, queryParam);
        queryStatement.setQueryParam(queryParam);
        queryStatement.setDataSetQueryParam(datasetQueryParam);
        // queryStatement.setMinMaxTime(queryStructUtils.getBeginEndTime(queryStructReq));
    }

    private AggOption getAggOption(QueryStatement queryStatement,
            List<MetricSchemaResp> metricSchemas) {
        String sql = queryStatement.getSql();
        if (!SqlSelectFunctionHelper.hasAggregateFunction(sql) && !SqlSelectHelper.hasGroupBy(sql)
                && !SqlSelectHelper.hasWith(sql) && !SqlSelectHelper.hasSubSelect(sql)) {
            log.debug("getAggOption simple sql set to DEFAULT");
            return AggOption.DEFAULT;
        }
        // if there is no group by in S2SQL,set MetricTable's aggOption to "NATIVE"
        // if there is count() in S2SQL,set MetricTable's aggOption to "NATIVE"
        if (!SqlSelectFunctionHelper.hasAggregateFunction(sql)
                || SqlSelectFunctionHelper.hasFunction(sql, "count")
                || SqlSelectFunctionHelper.hasFunction(sql, "count_distinct")) {
            return AggOption.OUTER;
        }
        // if (queryStatement.isInnerLayerNative()) {
        // return AggOption.NATIVE;
        // }
        if (SqlSelectHelper.hasSubSelect(sql) || SqlSelectHelper.hasWith(sql)
                || SqlSelectHelper.hasGroupBy(sql)) {
            return AggOption.OUTER;
        }
        long defaultAggNullCnt = metricSchemas.stream().filter(
                m -> Objects.isNull(m.getDefaultAgg()) || StringUtils.isBlank(m.getDefaultAgg()))
                .count();
        if (defaultAggNullCnt > 0) {
            log.debug("getAggOption find null defaultAgg metric set to NATIVE");
            return AggOption.OUTER;
        }
        return AggOption.DEFAULT;
    }

    private void convertNameToBizName(QueryStatement queryStatement) {
        SemanticSchemaResp semanticSchemaResp = queryStatement.getSemanticSchemaResp();
        Map<String, String> fieldNameToBizNameMap = getFieldNameToBizNameMap(semanticSchemaResp);
        String sql = queryStatement.getSql();
        log.debug("dataSetId:{},convert name to bizName before:{}", queryStatement.getDataSetId(),
                sql);
        sql = SqlReplaceHelper.replaceSqlByPositions(sql);
        log.debug("replaceSqlByPositions:{}", sql);
        sql = SqlReplaceHelper.replaceFields(sql, fieldNameToBizNameMap, true);
        log.debug("dataSetId:{},convert name to bizName after:{}", queryStatement.getDataSetId(),
                sql);
        sql = SqlReplaceHelper.replaceTable(sql,
                Constants.TABLE_PREFIX + queryStatement.getDataSetId());
        log.debug("replaceTableName after:{}", sql);
        queryStatement.setSql(sql);
    }

    private Set<String> getDimensions(SemanticSchemaResp semanticSchemaResp,
            List<String> allFields) {
        Map<String, String> dimensionLowerToNameMap = semanticSchemaResp.getDimensions().stream()
                .collect(Collectors.toMap(entry -> entry.getBizName().toLowerCase(),
                        SchemaItem::getBizName, (k1, k2) -> k1));
        dimensionLowerToNameMap.put(TimeDimensionEnum.DAY.getName(),
                TimeDimensionEnum.DAY.getName());
        return allFields.stream()
                .filter(entry -> dimensionLowerToNameMap.containsKey(entry.toLowerCase()))
                .map(entry -> dimensionLowerToNameMap.get(entry.toLowerCase()))
                .collect(Collectors.toSet());
    }

    private List<MetricSchemaResp> getMetrics(SemanticSchemaResp semanticSchemaResp,
            List<String> allFields) {
        Map<String, MetricSchemaResp> metricLowerToNameMap =
                semanticSchemaResp.getMetrics().stream().collect(Collectors
                        .toMap(entry -> entry.getBizName().toLowerCase(), entry -> entry));
        return allFields.stream()
                .filter(entry -> metricLowerToNameMap.containsKey(entry.toLowerCase()))
                .map(entry -> metricLowerToNameMap.get(entry.toLowerCase()))
                .collect(Collectors.toList());
    }

    private void rewriteFunction(QueryStatement queryStatement) {
        SemanticSchemaResp semanticSchemaResp = queryStatement.getSemanticSchemaResp();
        DatabaseResp database = semanticSchemaResp.getDatabaseResp();
        if (Objects.isNull(database) || Objects.isNull(database.getType())) {
            return;
        }
        String type = database.getType();
        DbAdaptor engineAdaptor = DbAdaptorFactory.getEngineAdaptor(type.toLowerCase());
        if (Objects.nonNull(engineAdaptor)) {
            String functionNameCorrector =
                    engineAdaptor.functionNameCorrector(queryStatement.getSql());
            queryStatement.setSql(functionNameCorrector);
        }
    }

    protected Map<String, String> getFieldNameToBizNameMap(SemanticSchemaResp semanticSchemaResp) {
        // support fieldName and field alias to bizName
        Map<String, String> dimensionResults = semanticSchemaResp.getDimensions().stream().flatMap(
                entry -> getPairStream(entry.getAlias(), entry.getName(), entry.getBizName()))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight, (k1, k2) -> k1));

        Map<String, String> metricResults = semanticSchemaResp.getMetrics().stream().flatMap(
                entry -> getPairStream(entry.getAlias(), entry.getName(), entry.getBizName()))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight, (k1, k2) -> k1));

        dimensionResults.putAll(TimeDimensionEnum.getChNameToNameMap());
        dimensionResults.putAll(TimeDimensionEnum.getNameToNameMap());
        dimensionResults.putAll(metricResults);
        return dimensionResults;
    }

    private Stream<Pair<String, String>> getPairStream(String aliasStr, String name,
            String bizName) {
        Set<Pair<String, String>> elements = new HashSet<>();
        elements.add(Pair.of(name, bizName));
        if (StringUtils.isNotBlank(aliasStr)) {
            List<String> aliasList = SchemaItem.getAliasList(aliasStr);
            for (String alias : aliasList) {
                elements.add(Pair.of(alias, bizName));
            }
        }
        return elements.stream();
    }

    private QueryType getQueryType(AggOption aggOption) {
        boolean isAgg = AggOption.isAgg(aggOption);
        QueryType queryType = QueryType.DETAIL;
        if (isAgg) {
            queryType = QueryType.AGGREGATE;
        }
        return queryType;
    }

    private void generateDerivedMetric(SemanticSchemaResp semanticSchemaResp, AggOption aggOption,
            DataSetQueryParam viewQueryParam) {
        String sql = viewQueryParam.getSql();
        for (MetricTable metricTable : viewQueryParam.getTables()) {
            Set<String> measures = new HashSet<>();
            Map<String, String> replaces = generateDerivedMetric(semanticSchemaResp, aggOption,
                    metricTable.getMetrics(), metricTable.getDimensions(), measures);

            if (!CollectionUtils.isEmpty(replaces)) {
                // metricTable sql use measures replace metric
                sql = SqlReplaceHelper.replaceSqlByExpression(sql, replaces);
                metricTable.setAggOption(AggOption.NATIVE);
                // metricTable use measures replace metric
                if (!CollectionUtils.isEmpty(measures)) {
                    metricTable.setMetrics(new ArrayList<>(measures));
                } else {
                    // empty measure , fill default
                    metricTable.setMetrics(new ArrayList<>());
                    metricTable.getMetrics().add(sqlGenerateUtils.generateInternalMetricName(
                            getDefaultModel(semanticSchemaResp, metricTable.getDimensions())));
                }
            }
        }
        viewQueryParam.setSql(sql);
    }

    private Map<String, String> generateDerivedMetric(SemanticSchemaResp semanticSchemaResp,
            AggOption aggOption, List<String> metrics, List<String> dimensions,
            Set<String> measures) {
        Map<String, String> result = new HashMap<>();
        List<MetricSchemaResp> metricResps = semanticSchemaResp.getMetrics();
        List<DimSchemaResp> dimensionResps = semanticSchemaResp.getDimensions();

        // Check if any metric is derived
        boolean hasDerivedMetrics =
                metricResps.stream().anyMatch(m -> metrics.contains(m.getBizName()) && MetricType
                        .isDerived(m.getMetricDefineType(), m.getMetricDefineByMeasureParams()));
        if (!hasDerivedMetrics) {
            return result;
        }

        log.debug("begin to generateDerivedMetric {} [{}]", aggOption, metrics);

        Set<String> allFields = new HashSet<>();
        Map<String, Measure> allMeasures = new HashMap<>();
        semanticSchemaResp.getModelResps().forEach(modelResp -> {
            allFields.addAll(modelResp.getFieldList());
            if (modelResp.getModelDetail().getMeasures() != null) {
                modelResp.getModelDetail().getMeasures()
                        .forEach(measure -> allMeasures.put(measure.getBizName(), measure));
            }
        });

        Set<String> derivedDimensions = new HashSet<>();
        Set<String> derivedMetrics = new HashSet<>();
        Map<String, String> visitedMetrics = new HashMap<>();

        for (MetricResp metricResp : metricResps) {
            if (metrics.contains(metricResp.getBizName())) {
                boolean isDerived = MetricType.isDerived(metricResp.getMetricDefineType(),
                        metricResp.getMetricDefineByMeasureParams());
                if (isDerived) {
                    String expr = sqlGenerateUtils.generateDerivedMetric(metricResps, allFields,
                            allMeasures, dimensionResps, sqlGenerateUtils.getExpr(metricResp),
                            metricResp.getMetricDefineType(), aggOption, visitedMetrics,
                            derivedMetrics, derivedDimensions);
                    result.put(metricResp.getBizName(), expr);
                    log.debug("derived metric {}->{}", metricResp.getBizName(), expr);
                } else {
                    measures.add(metricResp.getBizName());
                }
            }
        }

        measures.addAll(derivedMetrics);
        derivedDimensions.stream().filter(dimension -> !dimensions.contains(dimension))
                .forEach(dimensions::add);

        return result;
    }

    private String getDefaultModel(SemanticSchemaResp semanticSchemaResp, List<String> dimensions) {
        if (!CollectionUtils.isEmpty(dimensions)) {
            Map<String, Long> modelMatchCnt = new HashMap<>();
            for (ModelResp modelResp : semanticSchemaResp.getModelResps()) {
                modelMatchCnt.put(modelResp.getBizName(), modelResp.getModelDetail().getDimensions()
                        .stream().filter(d -> dimensions.contains(d.getBizName())).count());
            }
            return modelMatchCnt.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .map(m -> m.getKey()).findFirst().orElse("");
        }
        return semanticSchemaResp.getModelResps().get(0).getBizName();
    }
}
