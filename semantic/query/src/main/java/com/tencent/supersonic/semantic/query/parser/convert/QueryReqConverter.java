package com.tencent.supersonic.semantic.query.parser.convert;

import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserUpdateHelper;
import com.tencent.supersonic.semantic.api.model.enums.TimeDimensionEnum;
import com.tencent.supersonic.semantic.api.model.pojo.SchemaItem;
import com.tencent.supersonic.semantic.api.model.request.SqlExecuteReq;
import com.tencent.supersonic.semantic.api.model.response.DatabaseResp;
import com.tencent.supersonic.semantic.api.model.response.ModelSchemaResp;
import com.tencent.supersonic.semantic.api.query.pojo.MetricTable;
import com.tencent.supersonic.semantic.api.query.request.ParseSqlReq;
import com.tencent.supersonic.semantic.api.query.request.QueryDslReq;
import com.tencent.supersonic.semantic.model.domain.Catalog;
import com.tencent.supersonic.semantic.model.domain.ModelService;
import com.tencent.supersonic.semantic.model.domain.adaptor.engineadapter.EngineAdaptor;
import com.tencent.supersonic.semantic.model.domain.adaptor.engineadapter.EngineAdaptorFactory;
import com.tencent.supersonic.semantic.query.persistence.pojo.QueryStatement;
import com.tencent.supersonic.semantic.query.service.SemanticQueryEngine;
import com.tencent.supersonic.semantic.query.utils.QueryStructUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@Slf4j
public class QueryReqConverter {

    public static final String TABLE_PREFIX = "t_";
    @Autowired
    private ModelService domainService;
    @Autowired
    private SemanticQueryEngine parserService;
    @Autowired
    private QueryStructUtils queryStructUtils;

    @Autowired
    private Catalog catalog;

    public QueryStatement convert(QueryDslReq databaseReq, ModelSchemaResp modelSchemaResp) throws Exception {

        List<MetricTable> tables = new ArrayList<>();
        MetricTable metricTable = new MetricTable();

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

        List<String> allFields = SqlParserSelectHelper.getAllFields(databaseReq.getSql());

        List<String> metrics = getMetrics(modelSchemaResp, allFields);
        metricTable.setMetrics(metrics);

        Set<String> dimensions = getDimensions(modelSchemaResp, allFields);

        metricTable.setDimensions(new ArrayList<>(dimensions));

        metricTable.setAlias(tableName.toLowerCase());
        // if metric empty , fill model default
        if (CollectionUtils.isEmpty(metricTable.getMetrics())) {
            metricTable.setMetrics(new ArrayList<>(Arrays.asList(
                    queryStructUtils.generateInternalMetricName(databaseReq.getModelId(),
                            metricTable.getDimensions()))));
        }
        tables.add(metricTable);

        ParseSqlReq result = new ParseSqlReq();
        BeanUtils.copyProperties(databaseReq, result);
        result.setRootPath(domainService.getModelFullPathMap().get(databaseReq.getModelId()));
        result.setTables(tables);

        QueryStatement queryStatement = parserService.physicalSql(result);
        queryStatement.setSql(String.format(SqlExecuteReq.LIMIT_WRAPPER, queryStatement.getSql()));
        return queryStatement;
    }

    private void convertNameToBizName(QueryDslReq databaseReq, ModelSchemaResp modelSchemaResp) {
        Map<String, String> fieldNameToBizNameMap = getFieldNameToBizNameMap(modelSchemaResp);
        String sql = databaseReq.getSql();
        log.info("convert name to bizName before:{}", sql);
        String replaceFields = SqlParserUpdateHelper.replaceFields(sql, fieldNameToBizNameMap, false);
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

    private void functionNameCorrector(QueryDslReq databaseReq) {
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
        List<SchemaItem> allSchemaItems = new ArrayList<>();
        allSchemaItems.addAll(modelSchemaResp.getDimensions());
        allSchemaItems.addAll(modelSchemaResp.getMetrics());

        Map<String, String> result = allSchemaItems.stream()
                .collect(Collectors.toMap(SchemaItem::getName, a -> a.getBizName(), (k1, k2) -> k1));
        result.put(DateUtils.DATE_FIELD, TimeDimensionEnum.DAY.getName());
        return result;
    }

    public void correctTableName(QueryDslReq databaseReq) {
        String sql = SqlParserUpdateHelper.replaceTable(databaseReq.getSql(), TABLE_PREFIX + databaseReq.getModelId());
        databaseReq.setSql(sql);
    }

}
