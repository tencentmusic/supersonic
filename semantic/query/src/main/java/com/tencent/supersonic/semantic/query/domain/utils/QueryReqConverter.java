package com.tencent.supersonic.semantic.query.domain.utils;

import com.tencent.supersonic.common.util.calcite.SqlParseUtils;
import com.tencent.supersonic.common.util.calcite.SqlParserInfo;
import com.tencent.supersonic.semantic.api.core.response.DomainSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.SqlParserResp;
import com.tencent.supersonic.semantic.api.query.pojo.MetricTable;
import com.tencent.supersonic.semantic.api.query.request.ParseSqlReq;
import com.tencent.supersonic.semantic.api.query.request.QuerySqlReq;
import com.tencent.supersonic.semantic.core.domain.DomainService;
import com.tencent.supersonic.semantic.query.domain.SemanticQueryEngine;
import com.tencent.supersonic.semantic.query.domain.pojo.QueryStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class QueryReqConverter {

    @Autowired
    private DomainService domainService;

    @Autowired
    private SemanticQueryEngine parserService;

    public QueryStatement convert(QuerySqlReq databaseReq, List<DomainSchemaResp> domainSchemas) throws Exception {

        List<MetricTable> tables = new ArrayList<>();
        MetricTable metricTable = new MetricTable();
        String sql = databaseReq.getSql();
        SqlParserInfo sqlParseInfo = SqlParseUtils.getSqlParseInfo(sql);

        List<String> allFields = sqlParseInfo.getAllFields();

        if (CollectionUtils.isEmpty(domainSchemas)) {
            return new QueryStatement();
        }

        Set<String> dimensions = domainSchemas.get(0).getDimensions().stream()
                .map(entry -> entry.getBizName().toLowerCase())
                .collect(Collectors.toSet());
        dimensions.addAll(QueryStructUtils.internalCols);

        Set<String> metrics = domainSchemas.get(0).getMetrics().stream().map(entry -> entry.getBizName().toLowerCase())
                .collect(Collectors.toSet());

        metricTable.setMetrics(allFields.stream().filter(entry -> metrics.contains(entry.toLowerCase()))
                .map(entry -> entry.toLowerCase()).collect(Collectors.toList()));
        Set<String> collect = allFields.stream().filter(entry -> dimensions.contains(entry.toLowerCase()))
                .map(entry -> entry.toLowerCase()).collect(Collectors.toSet());
        for (String internalCol : QueryStructUtils.internalCols) {
            if (sql.contains(internalCol)) {
                collect.add(internalCol);
            }
        }
        metricTable.setDimensions(new ArrayList<>(collect));
        metricTable.setAlias(sqlParseInfo.getTableName().toLowerCase());
        tables.add(metricTable);

        ParseSqlReq result = new ParseSqlReq();
        BeanUtils.copyProperties(databaseReq, result);
        result.setRootPath(domainService.getDomainFullPath(databaseReq.getDomainId()));
        result.setTables(tables);

        return parserService.physicalSql(result);
    }

}
