package com.tencent.supersonic.semantic.query.parser.convert;


import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.Constants;
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
import java.util.ArrayList;
import java.util.Arrays;
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
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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

    public QueryStatement convert(QueryS2SQLReq databaseReq, ModelSchemaResp modelSchemaResp) throws Exception {

        if (Objects.isNull(modelSchemaResp)) {
            return new QueryStatement();
        }
        //1.convert name to bizName
        convertNameToBizName(databaseReq, modelSchemaResp);
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
        List<String> metrics = getMetrics(modelSchemaResp, allFields);
        QueryStructReq queryStructCmd = new QueryStructReq();
        MetricTable metricTable = new MetricTable();
        metricTable.setMetrics(metrics);

        Set<String> dimensions = getDimensions(modelSchemaResp, allFields);

        metricTable.setDimensions(new ArrayList<>(dimensions));

        metricTable.setAlias(tableName.toLowerCase());
        // if metric empty , fill model default
        if (CollectionUtils.isEmpty(metricTable.getMetrics())) {
            metricTable.setMetrics(new ArrayList<>(Arrays.asList(
                    queryStructUtils.generateInternalMetricName(databaseReq.getModelId(),
                            metricTable.getDimensions()))));
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
        result.setRootPath(domainService.getModelFullPathMap().get(databaseReq.getModelId()));
        result.setTables(tables);
        DatabaseResp database = catalog.getDatabaseByModelId(databaseReq.getModelId());
        if (!queryStructUtils.isSupportWith(EngineTypeEnum.valueOf(database.getType().toUpperCase()),
                database.getVersion())) {
            result.setSupportWith(false);
            result.setWithAlias(false);
        }
        //5.physicalSql by ParseSqlReq
        queryStructCmd.setDateInfo(queryStructUtils.getDateConfBySql(databaseReq.getSql()));
        queryStructCmd.setModelId(databaseReq.getModelId());
        queryStructCmd.setNativeQuery(!AggOption.isAgg(aggOption));
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

    private void convertNameToBizName(QueryS2SQLReq databaseReq, ModelSchemaResp modelSchemaResp) {
        Map<String, String> fieldNameToBizNameMap = getFieldNameToBizNameMap(modelSchemaResp);
        String sql = databaseReq.getSql();
        log.info("convert name to bizName before:{}", sql);
        String replaceFields = SqlParserReplaceHelper.replaceFields(sql, fieldNameToBizNameMap, true);
        log.info("convert name to bizName after:{}", replaceFields);
        databaseReq.setSql(replaceFields);
    }

    private Set<String> getDimensions(ModelSchemaResp modelSchemaResp, List<String> allFields) {
        Set<String> allDimensions = modelSchemaResp.getDimensions().stream()
                .map(entry -> entry.getBizName().toLowerCase())
                .collect(Collectors.toSet());
        allDimensions.addAll(QueryStructUtils.internalCols);
        Set<String> collect = allFields.stream().filter(entry -> allDimensions.contains(entry.toLowerCase()))
                .map(String::toLowerCase).collect(Collectors.toSet());
        return collect;
    }

    private List<String> getMetrics(ModelSchemaResp modelSchemaResp, List<String> allFields) {
        Set<String> allMetrics = modelSchemaResp.getMetrics().stream().map(entry -> entry.getBizName().toLowerCase())
                .collect(Collectors.toSet());
        List<String> metrics = allFields.stream().filter(entry -> allMetrics.contains(entry.toLowerCase()))
                .map(String::toLowerCase).collect(Collectors.toList());
        return metrics;
    }

    private void functionNameCorrector(QueryS2SQLReq databaseReq) {
        DatabaseResp database = catalog.getDatabaseByModelId(databaseReq.getModelId());
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


    protected Map<String, String> getFieldNameToBizNameMap(ModelSchemaResp modelSchemaResp) {
        // support fieldName and field alias to bizName
        Map<String, String> dimensionResults = modelSchemaResp.getDimensions().stream()
                .flatMap(entry -> getPairStream(entry.getAlias(), entry.getName(), entry.getBizName()))
                .collect(Collectors.toMap(a -> a.getLeft(), a -> a.getRight(), (k1, k2) -> k1));

        Map<String, String> metricResults = modelSchemaResp.getMetrics().stream()
                .flatMap(entry -> getPairStream(entry.getAlias(), entry.getName(), entry.getBizName()))
                .collect(Collectors.toMap(a -> a.getLeft(), a -> a.getRight(), (k1, k2) -> k1));

        dimensionResults.put(TimeDimensionEnum.DAY.getChName(), TimeDimensionEnum.DAY.getName());
        dimensionResults.put(TimeDimensionEnum.MONTH.getChName(), TimeDimensionEnum.MONTH.getName());
        dimensionResults.put(TimeDimensionEnum.WEEK.getChName(), TimeDimensionEnum.WEEK.getName());

        dimensionResults.put(TimeDimensionEnum.DAY.getName(), TimeDimensionEnum.DAY.getName());
        dimensionResults.put(TimeDimensionEnum.MONTH.getName(), TimeDimensionEnum.MONTH.getName());
        dimensionResults.put(TimeDimensionEnum.WEEK.getName(), TimeDimensionEnum.WEEK.getName());

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
        String sql = SqlParserReplaceHelper.replaceTable(databaseReq.getSql(),
                Constants.TABLE_PREFIX + databaseReq.getModelId());
        databaseReq.setSql(sql);
    }

}
