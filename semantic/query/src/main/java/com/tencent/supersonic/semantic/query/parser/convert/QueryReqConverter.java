package com.tencent.supersonic.semantic.query.parser.convert;


import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.QueryType;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserReplaceHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectFunctionHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import com.tencent.supersonic.semantic.api.model.pojo.SchemaItem;
import com.tencent.supersonic.semantic.api.model.request.SqlExecuteReq;
import com.tencent.supersonic.semantic.api.model.response.DatabaseResp;
import com.tencent.supersonic.semantic.api.model.response.ModelSchemaResp;
import com.tencent.supersonic.semantic.api.query.enums.AggOption;
import com.tencent.supersonic.semantic.api.query.pojo.MetricTable;
import com.tencent.supersonic.semantic.api.query.request.ParseSqlReq;
import com.tencent.supersonic.semantic.api.query.request.QueryS2SQLReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.semantic.model.domain.Catalog;
import com.tencent.supersonic.semantic.model.domain.ModelService;
import com.tencent.supersonic.semantic.model.domain.adaptor.engineadapter.EngineAdaptor;
import com.tencent.supersonic.semantic.model.domain.adaptor.engineadapter.EngineAdaptorFactory;
import com.tencent.supersonic.semantic.model.domain.pojo.EngineTypeEnum;
import com.tencent.supersonic.semantic.query.persistence.pojo.QueryStatement;
import com.tencent.supersonic.semantic.query.service.SemanticQueryEngine;
import com.tencent.supersonic.semantic.query.utils.QueryStructUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
public class QueryReqConverter {

    @Autowired
    private ModelService domainService;
    @Autowired
    private SemanticQueryEngine parserService;
    @Autowired
    private QueryStructUtils queryStructUtils;

    @Autowired
    private Catalog catalog;

    public QueryStatement convert(QueryS2SQLReq databaseReq, List<ModelSchemaResp> modelSchemaResps) throws Exception {

        if (CollectionUtils.isEmpty(modelSchemaResps)) {
            return new QueryStatement();
        }
        //1.convert name to bizName
        convertNameToBizName(databaseReq, modelSchemaResps);
        //2.functionName corrector
        functionNameCorrector(databaseReq);
        //3.correct tableName
        correctTableName(databaseReq);

        String tableName = SqlParserSelectHelper.getTableName(databaseReq.getSql());
        if (StringUtils.isEmpty(tableName)) {
            return new QueryStatement();
        }
        //4.build MetricTables
        List<String> allFields = SqlParserSelectHelper.getAllFields(databaseReq.getSql());
        List<String> metrics = getMetrics(modelSchemaResps, allFields);
        QueryStructReq queryStructCmd = new QueryStructReq();
        MetricTable metricTable = new MetricTable();
        metricTable.setMetrics(metrics);

        Set<String> dimensions = getDimensions(modelSchemaResps, allFields);

        metricTable.setDimensions(new ArrayList<>(dimensions));

        metricTable.setAlias(tableName.toLowerCase());
        // if metric empty , fill model default
        if (CollectionUtils.isEmpty(metricTable.getMetrics())) {
            metricTable.setMetrics(new ArrayList<>());
            for (Long modelId : databaseReq.getModelIds()) {
                metricTable.getMetrics().add(queryStructUtils.generateInternalMetricName(modelId,
                        metricTable.getDimensions()));
            }
        } else {
            queryStructCmd.setAggregators(
                    metricTable.getMetrics().stream().map(m -> new Aggregator(m, AggOperatorEnum.UNKNOWN)).collect(
                            Collectors.toList()));
        }
        AggOption aggOption = getAggOption(databaseReq);
        metricTable.setAggOption(aggOption);
        List<MetricTable> tables = new ArrayList<>();
        tables.add(metricTable);
        //4.build ParseSqlReq
        ParseSqlReq result = new ParseSqlReq();
        BeanUtils.copyProperties(databaseReq, result);

        result.setRootPath(catalog.getModelFullPath(databaseReq.getModelIds()));
        result.setTables(tables);
        DatabaseResp database = catalog.getDatabaseByModelId(databaseReq.getModelIds().get(0));
        if (!queryStructUtils.isSupportWith(EngineTypeEnum.valueOf(database.getType().toUpperCase()),
                database.getVersion())) {
            result.setSupportWith(false);
            result.setWithAlias(false);
        }
        //5.physicalSql by ParseSqlReq
        queryStructCmd.setDateInfo(queryStructUtils.getDateConfBySql(databaseReq.getSql()));
        queryStructCmd.setModelIds(databaseReq.getModelIds().stream().collect(Collectors.toSet()));
        queryStructCmd.setQueryType(getQueryType(aggOption));
        log.info("QueryReqConverter queryStructCmd[{}]", queryStructCmd);
        QueryStatement queryStatement = parserService.physicalSql(queryStructCmd, result);
        queryStatement.setSql(String.format(SqlExecuteReq.LIMIT_WRAPPER, queryStatement.getSql()));
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
        EngineAdaptor engineAdaptor = EngineAdaptorFactory.getEngineAdaptor(type.toLowerCase());
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

}
