package com.tencent.supersonic.semantic.query.service;

import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import com.tencent.supersonic.semantic.api.model.request.SqlExecuteReq;
import com.tencent.supersonic.semantic.api.model.response.ModelSchemaResp;
import com.tencent.supersonic.semantic.api.query.pojo.MetricTable;
import com.tencent.supersonic.semantic.api.query.request.ParseSqlReq;
import com.tencent.supersonic.semantic.api.query.request.QueryDslReq;
import com.tencent.supersonic.semantic.model.domain.ModelService;
import com.tencent.supersonic.semantic.query.persistence.pojo.QueryStatement;
import com.tencent.supersonic.semantic.query.utils.QueryStructUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class QueryReqConverter {

    @Autowired
    private ModelService domainService;
    @Autowired
    private SemanticQueryEngine parserService;
    @Autowired
    private QueryStructUtils queryStructUtils;

    public QueryStatement convert(QueryDslReq databaseReq, List<ModelSchemaResp> domainSchemas) throws Exception {

        List<MetricTable> tables = new ArrayList<>();
        MetricTable metricTable = new MetricTable();
        String sql = databaseReq.getSql();

        List<String> allFields = SqlParserSelectHelper.getAllFields(sql);
        String tableName = SqlParserSelectHelper.getTableName(sql);

        if (CollectionUtils.isEmpty(domainSchemas) || StringUtils.isEmpty(tableName)) {
            return new QueryStatement();
        }

        Set<String> dimensions = domainSchemas.get(0).getDimensions().stream()
                .map(entry -> entry.getBizName().toLowerCase())
                .collect(Collectors.toSet());
        dimensions.addAll(QueryStructUtils.internalCols);

        Set<String> metrics = domainSchemas.get(0).getMetrics().stream().map(entry -> entry.getBizName().toLowerCase())
                .collect(Collectors.toSet());

        metricTable.setMetrics(allFields.stream().filter(entry -> metrics.contains(entry.toLowerCase()))
                .map(String::toLowerCase).collect(Collectors.toList()));
        Set<String> collect = allFields.stream().filter(entry -> dimensions.contains(entry.toLowerCase()))
                .map(String::toLowerCase).collect(Collectors.toSet());
        for (String internalCol : QueryStructUtils.internalCols) {
            if (sql.contains(internalCol)) {
                collect.add(internalCol);
            }
        }
        metricTable.setDimensions(new ArrayList<>(collect));
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

}
