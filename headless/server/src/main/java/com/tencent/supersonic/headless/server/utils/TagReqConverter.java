package com.tencent.supersonic.headless.server.utils;

import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.util.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.headless.api.pojo.MetricTable;
import com.tencent.supersonic.headless.api.pojo.QueryParam;
import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import com.tencent.supersonic.headless.api.pojo.enums.EngineType;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryTagReq;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.pojo.ViewQueryParam;
import com.tencent.supersonic.headless.core.utils.SqlGenerateUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TagReqConverter {

    @Value("${query.sql.limitWrapper:true}")
    private Boolean limitWrapper;

    @Autowired
    private QueryStructUtils queryStructUtils;

    @Autowired
    private SqlGenerateUtils sqlGenerateUtils;

    public QueryStatement convert(QueryTagReq queryTagReq,
            SemanticSchemaResp semanticSchemaResp) throws Exception {
        QueryStatement queryStatement = new QueryStatement();
        // covert to QueryReqConverter
        QueryStructReq queryStructReq = new QueryStructReq();
        BeanUtils.copyProperties(queryTagReq, queryStructReq);
        if (!CollectionUtils.isEmpty(queryTagReq.getTagFilters())) {
            queryStructReq.setDimensionFilters(queryTagReq.getTagFilters());
        }
        QuerySqlReq querySqlReq = queryStructReq.convert();
        if (Objects.nonNull(querySqlReq)) {
            log.info("convert to QuerySqlReq {}", querySqlReq);
            String tableName = SqlSelectHelper.getTableName(querySqlReq.getSql());
            MetricTable metricTable = new MetricTable();
            metricTable.setMetrics(new ArrayList<>());
            metricTable.getMetrics().add(sqlGenerateUtils.generateInternalMetricName(
                    semanticSchemaResp.getModelResps().get(0).getBizName()));
            metricTable.setAggOption(AggOption.NATIVE);
            List<String> allFields = SqlSelectHelper.getAllFields(querySqlReq.getSql());
            metricTable.setDimensions(allFields);
            metricTable.setAlias(tableName.toLowerCase());
            List<MetricTable> tables = new ArrayList<>();
            tables.add(metricTable);
            //.build ParseSqlReq
            ViewQueryParam result = new ViewQueryParam();
            BeanUtils.copyProperties(querySqlReq, result);
            result.setTables(tables);
            DatabaseResp database = semanticSchemaResp.getDatabaseResp();
            if (!sqlGenerateUtils.isSupportWith(EngineType.fromString(database.getType().toUpperCase()),
                    database.getVersion())) {
                result.setSupportWith(false);
                result.setWithAlias(false);
            }
            //.physicalSql by ParseSqlReq
            queryStructReq.setDateInfo(queryStructUtils.getDateConfBySql(querySqlReq.getSql()));
            queryStructReq.setViewId(querySqlReq.getViewId());
            queryStructReq.setQueryType(QueryType.TAG);
            QueryParam queryParam = new QueryParam();
            convert(queryTagReq, queryParam);
            queryStatement.setQueryParam(queryParam);
            queryStatement.setViewQueryParam(result);
            queryStatement.setIsS2SQL(true);
            queryStatement.setMinMaxTime(queryStructUtils.getBeginEndTime(queryStructReq));
            queryStatement.setViewId(queryTagReq.getViewId());
            queryStatement.setEnableLimitWrapper(limitWrapper);
        }
        return queryStatement;
    }

    public void convert(QueryTagReq queryTagReq, QueryParam queryParam) {
        BeanUtils.copyProperties(queryTagReq, queryParam);
        queryParam.setOrders(queryTagReq.getOrders());
        queryParam.setMetrics(queryTagReq.getMetrics());
        queryParam.setGroups(queryTagReq.getGroups());
        queryParam.setDimensionFilters(queryTagReq.getTagFilters());
        queryParam.setQueryType(QueryType.TAG);
    }
}
