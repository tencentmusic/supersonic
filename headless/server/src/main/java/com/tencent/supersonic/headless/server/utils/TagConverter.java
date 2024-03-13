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
import com.tencent.supersonic.headless.api.pojo.response.DataSetResp;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.TagResp;
import com.tencent.supersonic.headless.core.pojo.DataSetQueryParam;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.utils.SqlGenerateUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TagConverter {

    @Value("${query.sql.limitWrapper:true}")
    private Boolean limitWrapper;

    @Autowired
    private QueryStructUtils queryStructUtils;

    @Autowired
    private SqlGenerateUtils sqlGenerateUtils;

    public QueryStatement convert(QueryStructReq queryStructReq,
            SemanticSchemaResp semanticSchemaResp) throws Exception {
        QueryStatement queryStatement = new QueryStatement();
        // covert to QueryReqConverter
        QueryStructReq queryStructReq = new QueryStructReq();
        BeanUtils.copyProperties(queryTagReq, queryStructReq);
        // queryStructReq.setModelIds(queryTagReq.getModelIdSet());
        if (!CollectionUtils.isEmpty(queryTagReq.getTagFilters())) {
            queryStructReq.setDimensionFilters(queryTagReq.getTagFilters());
        }
        QuerySqlReq querySqlReq = queryStructReq.convert();
        convert(querySqlReq, semanticSchemaResp, queryStatement, queryStructReq);
        QueryParam queryParam = new QueryParam();
        convert(queryStructReq, queryParam);
        queryStatement.setQueryParam(queryParam);
        queryStatement.setDataSetId(queryStructReq.getDataSetId());
        return queryStatement;
    }

    public void convert(QuerySqlReq querySqlReq,
            SemanticSchemaResp semanticSchemaResp, QueryStatement queryStatement, QueryStructReq queryStructReq)
            throws Exception {
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
            DataSetQueryParam result = new DataSetQueryParam();
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
            queryStructReq.setDataSetId(querySqlReq.getDataSetId());
            queryStructReq.setQueryType(QueryType.TAG);

            queryStatement.setDataSetQueryParam(result);
            queryStatement.setIsS2SQL(true);
            queryStatement.setMinMaxTime(queryStructUtils.getBeginEndTime(queryStructReq));

            queryStatement.setEnableLimitWrapper(limitWrapper);
        }
    }

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
        convert(querySqlReq, semanticSchemaResp, queryStatement, queryStructReq);
        QueryParam queryParam = new QueryParam();
        convert(queryTagReq, queryParam);
        queryStatement.setQueryParam(queryParam);
        queryStatement.setDataSetId(queryTagReq.getDataSetId());
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

    public void convert(QueryStructReq queryTagReq, QueryParam queryParam) {
        BeanUtils.copyProperties(queryTagReq, queryParam);
        queryParam.setOrders(queryTagReq.getOrders());
        queryParam.setMetrics(queryTagReq.getMetrics());
        queryParam.setGroups(queryTagReq.getGroups());
        queryParam.setDimensionFilters(queryTagReq.getDimensionFilters());
        queryParam.setQueryType(QueryType.TAG);
    }

    public static List<TagResp> filterByDataSet(List<TagResp> tagResps, DataSetResp dataSetResp) {
        return tagResps.stream().filter(tagResp -> dataSetResp.getAllTags().contains(tagResp.getId())
                || dataSetResp.getAllIncludeAllModels().contains(tagResp.getModelId())).collect(Collectors.toList());
    }
}
