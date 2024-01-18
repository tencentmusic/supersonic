package com.tencent.supersonic.headless.server.utils;


import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserReplaceHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectFunctionHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import com.tencent.supersonic.headless.api.enums.AggOption;
import com.tencent.supersonic.headless.api.enums.EngineType;
import com.tencent.supersonic.headless.api.enums.MetricType;
import com.tencent.supersonic.headless.api.pojo.Measure;
import com.tencent.supersonic.headless.api.pojo.MetricTable;
import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import com.tencent.supersonic.headless.api.request.ParseSqlReq;
import com.tencent.supersonic.headless.api.request.QueryS2SQLReq;
import com.tencent.supersonic.headless.api.request.QueryStructReq;
import com.tencent.supersonic.headless.api.request.SqlExecuteReq;
import com.tencent.supersonic.headless.api.response.DatabaseResp;
import com.tencent.supersonic.headless.api.response.DimensionResp;
import com.tencent.supersonic.headless.api.response.MetricResp;
import com.tencent.supersonic.headless.api.response.ModelSchemaResp;
import com.tencent.supersonic.headless.core.adaptor.db.DbAdaptor;
import com.tencent.supersonic.headless.core.adaptor.db.DbAdaptorFactory;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.utils.SqlGenerateUtils;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;
import com.tencent.supersonic.headless.server.service.Catalog;
import com.tencent.supersonic.headless.server.service.HeadlessQueryEngine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@Slf4j
public class QueryReqConverter {

    @Value("${query.sql.limitWrapper:true}")
    private Boolean limitWrapper;

    @Autowired
    private HeadlessQueryEngine headlessQueryEngine;
    @Autowired
    private QueryStructUtils queryStructUtils;

    @Autowired
    private SqlGenerateUtils sqlGenerateUtils;

    @Autowired
    private Catalog catalog;

    public QueryStatement convert(QueryS2SQLReq queryS2SQLReq,
            List<ModelSchemaResp> modelSchemaResps) throws Exception {

        if (CollectionUtils.isEmpty(modelSchemaResps)) {
            return new QueryStatement();
        }
        Map<Long, ModelSchemaResp> modelSchemaRespMap = modelSchemaResps.stream()
                .collect(Collectors.toMap(ModelSchemaResp::getId, modelSchemaResp -> modelSchemaResp));
        //1.convert name to bizName
        convertNameToBizName(queryS2SQLReq, modelSchemaResps);
        //2.functionName corrector
        functionNameCorrector(queryS2SQLReq);
        //3.correct tableName
        correctTableName(queryS2SQLReq);

        String tableName = SqlParserSelectHelper.getTableName(queryS2SQLReq.getSql());
        if (StringUtils.isEmpty(tableName)) {
            return new QueryStatement();
        }
        //4.build MetricTables
        List<String> allFields = SqlParserSelectHelper.getAllFields(queryS2SQLReq.getSql());
        List<String> metrics = getMetrics(modelSchemaResps, allFields);
        QueryStructReq queryStructReq = new QueryStructReq();
        MetricTable metricTable = new MetricTable();
        metricTable.setMetrics(metrics);

        Set<String> dimensions = getDimensions(modelSchemaResps, allFields);

        metricTable.setDimensions(new ArrayList<>(dimensions));

        metricTable.setAlias(tableName.toLowerCase());
        // if metric empty , fill model default
        if (CollectionUtils.isEmpty(metricTable.getMetrics())) {
            metricTable.setMetrics(new ArrayList<>());
            for (Long modelId : queryS2SQLReq.getModelIds()) {
                ModelSchemaResp modelSchemaResp = modelSchemaRespMap.get(modelId);
                metricTable.getMetrics().add(sqlGenerateUtils.generateInternalMetricName(modelSchemaResp.getBizName()));
            }
        } else {
            queryStructReq.setAggregators(
                    metricTable.getMetrics().stream().map(m -> new Aggregator(m, AggOperatorEnum.UNKNOWN)).collect(
                            Collectors.toList()));
        }
        AggOption aggOption = getAggOption(queryS2SQLReq);
        metricTable.setAggOption(aggOption);
        List<MetricTable> tables = new ArrayList<>();
        tables.add(metricTable);
        //4.build ParseSqlReq
        ParseSqlReq result = new ParseSqlReq();
        BeanUtils.copyProperties(queryS2SQLReq, result);

        result.setRootPath(queryS2SQLReq.getModelIdStr());
        result.setTables(tables);
        DatabaseResp database = catalog.getDatabaseByModelId(queryS2SQLReq.getModelIds().get(0));
        if (!sqlGenerateUtils.isSupportWith(EngineType.valueOf(database.getType().toUpperCase()),
                database.getVersion())) {
            result.setSupportWith(false);
            result.setWithAlias(false);
        }
        //5. do deriveMetric
        generateDerivedMetric(queryS2SQLReq.getModelIds(), modelSchemaResps, result);
        //6.physicalSql by ParseSqlReq
        queryStructReq.setDateInfo(queryStructUtils.getDateConfBySql(queryS2SQLReq.getSql()));
        queryStructReq.setModelIds(new HashSet<>(queryS2SQLReq.getModelIds()));
        queryStructReq.setQueryType(getQueryType(aggOption));
        log.info("QueryReqConverter queryStructReq[{}]", queryStructReq);
        QueryStatement queryStatement = new QueryStatement();
        queryStatement.setQueryStructReq(queryStructReq);
        queryStatement.setParseSqlReq(result);
        queryStatement.setIsS2SQL(true);
        queryStatement.setMinMaxTime(queryStructUtils.getBeginEndTime(queryStructReq));
        queryStatement.setModelIds(queryS2SQLReq.getModelIds());
        queryStatement = headlessQueryEngine.plan(queryStatement);
        queryStatement.setSql(limitWrapper ? String.format(SqlExecuteReq.LIMIT_WRAPPER, queryStatement.getSql())
                : queryStatement.getSql());
        return queryStatement;
    }

    private AggOption getAggOption(QueryS2SQLReq databaseReq) {
        // if there is no group by in S2SQL,set MetricTable's aggOption to "NATIVE"
        // if there is count() in S2SQL,set MetricTable's aggOption to "NATIVE"
        String sql = databaseReq.getSql();
        if (!SqlParserSelectHelper.hasGroupBy(sql)
                || SqlParserSelectFunctionHelper.hasFunction(sql, "count")
                || SqlParserSelectFunctionHelper.hasFunction(sql, "count_distinct")) {
            return AggOption.NATIVE;
        }
        return AggOption.DEFAULT;
    }

    private void convertNameToBizName(QueryS2SQLReq databaseReq, List<ModelSchemaResp> modelSchemaResps) {
        Map<String, String> fieldNameToBizNameMap = getFieldNameToBizNameMap(modelSchemaResps);
        String sql = databaseReq.getSql();
        log.info("convert name to bizName before:{}", sql);
        String replaceFields = SqlParserReplaceHelper.replaceFields(sql, fieldNameToBizNameMap, true);
        log.info("convert name to bizName after:{}", replaceFields);
        databaseReq.setSql(replaceFields);
    }

    private Set<String> getDimensions(List<ModelSchemaResp> modelSchemaResps, List<String> allFields) {
        Map<String, String> dimensionLowerToNameMap = modelSchemaResps.stream()
                .flatMap(modelSchemaResp -> modelSchemaResp.getDimensions().stream())
                .collect(Collectors.toMap(entry -> entry.getBizName().toLowerCase(), SchemaItem::getBizName,
                        (k1, k2) -> k1));
        Map<String, String> internalLowerToNameMap = QueryStructUtils.internalCols.stream()
                .collect(Collectors.toMap(String::toLowerCase, a -> a));
        dimensionLowerToNameMap.putAll(internalLowerToNameMap);
        return allFields.stream()
                .filter(entry -> dimensionLowerToNameMap.containsKey(entry.toLowerCase()))
                .map(entry -> dimensionLowerToNameMap.get(entry.toLowerCase())).collect(Collectors.toSet());
    }

    private List<String> getMetrics(List<ModelSchemaResp> modelSchemaResps, List<String> allFields) {
        Map<String, String> metricLowerToNameMap = modelSchemaResps.stream()
                .flatMap(modelSchemaResp -> modelSchemaResp.getMetrics().stream())
                .collect(Collectors.toMap(entry -> entry.getBizName().toLowerCase(), SchemaItem::getBizName));
        return allFields.stream().filter(entry -> metricLowerToNameMap.containsKey(entry.toLowerCase()))
                .map(entry -> metricLowerToNameMap.get(entry.toLowerCase())).collect(Collectors.toList());
    }

    private void functionNameCorrector(QueryS2SQLReq databaseReq) {
        DatabaseResp database = catalog.getDatabaseByModelId(databaseReq.getModelIds().get(0));
        if (Objects.isNull(database) || Objects.isNull(database.getType())) {
            return;
        }
        String type = database.getType();
        DbAdaptor engineAdaptor = DbAdaptorFactory.getEngineAdaptor(type.toLowerCase());
        log.info("type:{},engineAdaptor:{}", type, engineAdaptor);
        if (Objects.nonNull(engineAdaptor)) {
            String functionNameCorrector = engineAdaptor.functionNameCorrector(databaseReq.getSql());
            log.info("sql:{} ,after corrector", databaseReq.getSql(), functionNameCorrector);
            databaseReq.setSql(functionNameCorrector);
        }
    }

    protected Map<String, String> getFieldNameToBizNameMap(List<ModelSchemaResp> modelSchemaResps) {
        // support fieldName and field alias to bizName
        Map<String, String> dimensionResults = modelSchemaResps.stream().flatMap(modelSchemaResp
                        -> modelSchemaResp.getDimensions().stream())
                .flatMap(entry -> getPairStream(entry.getAlias(), entry.getName(), entry.getBizName()))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight, (k1, k2) -> k1));

        Map<String, String> metricResults = modelSchemaResps.stream().flatMap(modelSchemaResp
                        -> modelSchemaResp.getMetrics().stream())
                .flatMap(entry -> getPairStream(entry.getAlias(), entry.getName(), entry.getBizName()))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight, (k1, k2) -> k1));

        dimensionResults.putAll(TimeDimensionEnum.getChNameToNameMap());
        dimensionResults.putAll(TimeDimensionEnum.getNameToNameMap());
        dimensionResults.putAll(metricResults);
        return dimensionResults;
    }

    private Stream<Pair<String, String>> getPairStream(String aliasStr, String name, String bizName) {
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

    public void correctTableName(QueryS2SQLReq databaseReq) {
        String sql = databaseReq.getSql();
        for (Long modelId : databaseReq.getModelIds()) {
            sql = SqlParserReplaceHelper.replaceTable(sql, Constants.TABLE_PREFIX + modelId);
        }
        databaseReq.setSql(sql);
    }

    private QueryType getQueryType(AggOption aggOption) {
        boolean isAgg = AggOption.isAgg(aggOption);
        QueryType queryType = QueryType.TAG;
        if (isAgg) {
            queryType = QueryType.METRIC;
        }
        return queryType;
    }

    private void generateDerivedMetric(List<Long> modelIds, List<ModelSchemaResp> modelSchemaResps,
            ParseSqlReq parseSqlReq) {
        String sql = parseSqlReq.getSql();
        for (MetricTable metricTable : parseSqlReq.getTables()) {
            List<String> measures = new ArrayList<>();
            Map<String, String> replaces = new HashMap<>();
            generateDerivedMetric(modelIds, modelSchemaResps, metricTable.getMetrics(), metricTable.getDimensions(),
                    measures, replaces);
            if (!CollectionUtils.isEmpty(replaces)) {
                // metricTable sql use measures replace metric
                sql = SqlParserReplaceHelper.replaceSqlByExpression(sql, replaces);
                metricTable.setAggOption(AggOption.NATIVE);
            }
            // metricTable  use measures replace metric
            if (!CollectionUtils.isEmpty(measures)) {
                metricTable.setMetrics(measures);
            }
        }
        parseSqlReq.setSql(sql);
    }

    private void generateDerivedMetric(List<Long> modelIds, List<ModelSchemaResp> modelSchemaResps,
            List<String> metrics, List<String> dimensions,
            List<String> measures, Map<String, String> replaces) {
        MetaFilter metaFilter = new MetaFilter();
        metaFilter.setModelIds(modelIds);
        List<MetricResp> metricResps = catalog.getMetrics(metaFilter);
        List<DimensionResp> dimensionResps = catalog.getDimensions(metaFilter);
        // check metrics has derived
        if (!metricResps.stream()
                .anyMatch(m -> metrics.contains(m.getBizName()) && MetricType.isDerived(m.getMetricDefineType(),
                        m.getTypeParams()))) {
            return;
        }
        Set<String> allFields = new HashSet<>();
        Map<String, Measure> allMeasures = new HashMap<>();
        modelSchemaResps.stream().forEach(modelSchemaResp -> {
            allFields.addAll(modelSchemaResp.getFieldList());
            if (Objects.nonNull(modelSchemaResp.getModelDetail().getMeasures())) {
                modelSchemaResp.getModelDetail().getMeasures().stream()
                        .forEach(mm -> allMeasures.put(mm.getBizName(), mm));
            }
        });

        Set<String> deriveDimension = new HashSet<>();
        Set<String> deriveMetric = new HashSet<>();
        Set<String> visitedMetric = new HashSet<>();
        if (!CollectionUtils.isEmpty(metricResps)) {
            for (MetricResp metricResp : metricResps) {
                if (metrics.contains(metricResp.getBizName())) {
                    if (MetricType.isDerived(metricResp.getMetricDefineType(), metricResp.getTypeParams())) {
                        String expr = sqlGenerateUtils.generateDerivedMetric(metricResps, allFields, allMeasures,
                                dimensionResps,
                                sqlGenerateUtils.getExpr(metricResp), metricResp.getMetricDefineType(), visitedMetric,
                                deriveMetric, deriveDimension);
                        replaces.put(metricResp.getBizName(), expr);
                    } else {
                        measures.add(metricResp.getBizName());
                    }
                }
            }
        }
        measures.addAll(deriveMetric);
        dimensions.addAll(deriveDimension);
    }

}
