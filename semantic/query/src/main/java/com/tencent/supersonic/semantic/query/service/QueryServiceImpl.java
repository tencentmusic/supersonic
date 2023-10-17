package com.tencent.supersonic.semantic.query.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.util.FileUtils;
import com.google.common.cache.CacheBuilder;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.enums.TaskStatusEnum;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.common.util.cache.CacheUtils;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.semantic.api.model.enums.QueryTypeEnum;
import com.tencent.supersonic.semantic.api.model.request.ModelSchemaFilterReq;
import com.tencent.supersonic.semantic.api.model.response.ExplainResp;
import com.tencent.supersonic.semantic.api.model.response.ModelSchemaResp;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;
import com.tencent.supersonic.semantic.api.query.pojo.Cache;
import com.tencent.supersonic.semantic.api.query.pojo.Filter;
import com.tencent.supersonic.semantic.api.query.request.ExplainSqlReq;
import com.tencent.supersonic.semantic.api.query.request.ItemUseReq;
import com.tencent.supersonic.semantic.api.query.request.MetricReq;
import com.tencent.supersonic.semantic.api.query.request.QueryDimValueReq;
import com.tencent.supersonic.semantic.api.query.request.QueryS2QLReq;
import com.tencent.supersonic.semantic.api.query.request.QueryMultiStructReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.semantic.api.query.response.ItemUseResp;
import com.tencent.supersonic.semantic.query.utils.S2QLPermissionAnnotation;
import com.tencent.supersonic.semantic.query.executor.QueryExecutor;
import com.tencent.supersonic.semantic.query.parser.convert.QueryReqConverter;
import com.tencent.supersonic.semantic.query.persistence.pojo.QueryStatement;
import com.tencent.supersonic.semantic.query.utils.QueryUtils;
import com.tencent.supersonic.semantic.query.utils.StatUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletResponse;


@Service
@Slf4j
public class QueryServiceImpl implements QueryService {

    protected final com.google.common.cache.Cache<String, List<ItemUseResp>> itemUseCache =
            CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).build();

    private final StatUtils statUtils;
    private final CacheUtils cacheUtils;
    private final QueryUtils queryUtils;
    private final QueryReqConverter queryReqConverter;

    @Value("${query.cache.enable:true}")
    private Boolean cacheEnable;

    private final SemanticQueryEngine semanticQueryEngine;

    public QueryServiceImpl(
            StatUtils statUtils,
            CacheUtils cacheUtils,
            QueryUtils queryUtils,
            QueryReqConverter queryReqConverter,
            SemanticQueryEngine semanticQueryEngine) {
        this.statUtils = statUtils;
        this.cacheUtils = cacheUtils;
        this.queryUtils = queryUtils;
        this.queryReqConverter = queryReqConverter;
        this.semanticQueryEngine = semanticQueryEngine;
    }

    @Override
    @S2QLPermissionAnnotation
    @SneakyThrows
    public Object queryBySql(QueryS2QLReq querySqlCmd, User user) {
        statUtils.initStatInfo(querySqlCmd, user);
        QueryStatement queryStatement = new QueryStatement();
        try {
            queryStatement = convertToQueryStatement(querySqlCmd, user);
        } catch (Exception e) {
            log.info("convertToQueryStatement has a exception:{}", e.toString());
        }
        log.info("queryStatement:{}", queryStatement);
        QueryResultWithSchemaResp results = semanticQueryEngine.execute(queryStatement);
        statUtils.statInfo2DbAsync(TaskStatusEnum.SUCCESS);
        return results;
    }

    public Object queryByQueryStatement(QueryStatement queryStatement) {
        return semanticQueryEngine.execute(queryStatement);
    }

    private QueryStatement convertToQueryStatement(QueryS2QLReq querySqlCmd, User user) throws Exception {
        ModelSchemaFilterReq filter = new ModelSchemaFilterReq();
        List<Long> modelIds = new ArrayList<>();
        modelIds.add(querySqlCmd.getModelId());

        filter.setModelIds(modelIds);
        SchemaService schemaService = ContextUtils.getBean(SchemaService.class);
        List<ModelSchemaResp> domainSchemas = schemaService.fetchModelSchema(filter, user);
        ModelSchemaResp domainSchema = null;
        if (CollectionUtils.isNotEmpty(domainSchemas)) {
            domainSchema = domainSchemas.get(0);
        }
        QueryStatement queryStatement = queryReqConverter.convert(querySqlCmd, domainSchema);
        queryStatement.setModelId(querySqlCmd.getModelId());
        return queryStatement;
    }

    @Override
    public QueryResultWithSchemaResp queryByStruct(QueryStructReq queryStructCmd, User user) throws Exception {
        QueryResultWithSchemaResp queryResultWithColumns = null;
        log.info("[queryStructCmd:{}]", queryStructCmd);
        try {
            statUtils.initStatInfo(queryStructCmd, user);
            String cacheKey = cacheUtils.generateCacheKey(queryStructCmd.getModelId().toString(),
                    queryStructCmd.generateCommandMd5());
            handleGlobalCacheDisable(queryStructCmd);
            boolean isCache = isCache(queryStructCmd);
            if (isCache) {
                queryResultWithColumns = queryByCache(cacheKey, queryStructCmd);
                if (queryResultWithColumns != null) {
                    statUtils.statInfo2DbAsync(TaskStatusEnum.SUCCESS);
                    return queryResultWithColumns;
                }
            }
            StatUtils.get().setUseResultCache(false);
            QueryStatement queryStatement = semanticQueryEngine.plan(queryStructCmd);
            QueryExecutor queryExecutor = semanticQueryEngine.route(queryStatement);
            if (queryExecutor != null) {
                queryResultWithColumns = semanticQueryEngine.execute(queryStatement);
                if (isCache) {
                    // if queryResultWithColumns is not null, update cache data
                    queryUtils.cacheResultLogic(cacheKey, queryResultWithColumns);
                }
            }
            statUtils.statInfo2DbAsync(TaskStatusEnum.SUCCESS);
            return queryResultWithColumns;
        } catch (Exception e) {
            log.warn("exception in queryByStruct, e: ", e);
            statUtils.statInfo2DbAsync(TaskStatusEnum.ERROR);
            throw e;
        }
    }

    @Override
    public void downloadByStruct(QueryStructReq queryStructReq,
                                 User user, HttpServletResponse response) throws Exception {
        QueryResultWithSchemaResp queryResultWithSchemaResp = queryByStruct(queryStructReq, user);
        List<List<String>> data = new ArrayList<>();
        List<List<String>> header = Lists.newArrayList();
        for (QueryColumn column : queryResultWithSchemaResp.getColumns()) {
            header.add(Lists.newArrayList(column.getName()));
        }
        for (Map<String, Object> row : queryResultWithSchemaResp.getResultList()) {
            List<String> rowData = new ArrayList<>();
            for (QueryColumn column : queryResultWithSchemaResp.getColumns()) {
                rowData.add(String.valueOf(row.get(column.getNameEn())));
            }
            data.add(rowData);
        }
        String fileName = String.format("%s_%s.xlsx", "supersonic", DateUtils.format(new Date(), DateUtils.FORMAT));
        File file = FileUtils.createTmpFile(fileName);
        EasyExcel.write(file).sheet("Sheet1").head(header).doWrite(data);
        downloadFile(response, file, fileName);
    }

    private void downloadFile(HttpServletResponse response, File file, String filename) {
        try {
            byte[] buffer = readFileToByteArray(file);
            response.reset();
            response.setCharacterEncoding("UTF-8");
            response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, "UTF-8"));
            response.addHeader("Content-Length", "" + file.length());
            try (OutputStream outputStream = new BufferedOutputStream(response.getOutputStream())) {
                response.setContentType("application/octet-stream");
                outputStream.write(buffer);
                outputStream.flush();
            }
        } catch (Exception e) {
            log.error("failed to download file", e);
        }
    }

    private byte[] readFileToByteArray(File file) throws IOException {
        try (InputStream fis = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            return buffer;
        }
    }

    @Override
    @DataPermission
    @SneakyThrows
    public QueryResultWithSchemaResp queryByStructWithAuth(QueryStructReq queryStructCmd, User user) {
        return queryByStruct(queryStructCmd, user);
    }


    @Override
    public QueryResultWithSchemaResp queryByMultiStruct(QueryMultiStructReq queryMultiStructReq, User user)
            throws Exception {
        statUtils.initStatInfo(queryMultiStructReq.getQueryStructReqs().get(0), user);
        String cacheKey = cacheUtils.generateCacheKey(
                queryMultiStructReq.getQueryStructReqs().get(0).getModelId().toString(),
                queryMultiStructReq.generateCommandMd5());
        boolean isCache = isCache(queryMultiStructReq);
        QueryResultWithSchemaResp queryResultWithColumns;
        if (isCache) {
            queryResultWithColumns = queryByCache(cacheKey, queryMultiStructReq);
            if (queryResultWithColumns != null) {
                statUtils.statInfo2DbAsync(TaskStatusEnum.SUCCESS);
                return queryResultWithColumns;
            }
        }
        log.info("stat queryByStructWithoutCache, queryMultiStructReq:{}", queryMultiStructReq);
        try {
            QueryStatement sqlParser = getQueryStatementByMultiStruct(queryMultiStructReq);
            queryResultWithColumns = semanticQueryEngine.execute(sqlParser);
            if (queryResultWithColumns != null) {
                statUtils.statInfo2DbAsync(TaskStatusEnum.SUCCESS);
                queryUtils.fillItemNameInfo(queryResultWithColumns, queryMultiStructReq);
            }
            return queryResultWithColumns;
        } catch (Exception e) {
            log.warn("exception in queryByMultiStruct, e: ", e);
            statUtils.statInfo2DbAsync(TaskStatusEnum.ERROR);
            throw e;
        }
    }

    private QueryStatement getQueryStatementByMultiStruct(QueryMultiStructReq queryMultiStructReq) throws Exception {
        List<QueryStatement> sqlParsers = new ArrayList<>();
        for (QueryStructReq queryStructCmd : queryMultiStructReq.getQueryStructReqs()) {
            QueryStatement queryStatement = semanticQueryEngine.plan(queryStructCmd);
            queryUtils.checkSqlParse(queryStatement);
            sqlParsers.add(queryStatement);
        }
        log.info("multi sqlParser:{}", sqlParsers);
        return queryUtils.sqlParserUnion(queryMultiStructReq, sqlParsers);
    }

    @Override
    @SneakyThrows
    public QueryResultWithSchemaResp queryDimValue(QueryDimValueReq queryDimValueReq, User user) {
        QueryStructReq queryStructReq = generateDimValueQueryStruct(queryDimValueReq);
        return queryByStruct(queryStructReq, user);
    }


    private void handleGlobalCacheDisable(QueryStructReq queryStructCmd) {
        if (!cacheEnable) {
            Cache cacheInfo = new Cache();
            cacheInfo.setCache(false);
            queryStructCmd.setCacheInfo(cacheInfo);
        }
    }

    @Override
    @SneakyThrows
    public List<ItemUseResp> getStatInfo(ItemUseReq itemUseReq) {
        if (itemUseReq.getCacheEnable()) {
            return itemUseCache.get(JsonUtil.toString(itemUseReq), () -> {
                List<ItemUseResp> data = statUtils.getStatInfo(itemUseReq);
                itemUseCache.put(JsonUtil.toString(itemUseReq), data);
                return data;
            });
        }
        return statUtils.getStatInfo(itemUseReq);
    }

    @Override
    public <T> ExplainResp explain(ExplainSqlReq<T> explainSqlReq, User user) throws Exception {
        QueryTypeEnum queryTypeEnum = explainSqlReq.getQueryTypeEnum();
        T queryReq = explainSqlReq.getQueryReq();

        if (QueryTypeEnum.SQL.equals(queryTypeEnum) && queryReq instanceof QueryS2QLReq) {
            QueryStatement queryStatement = convertToQueryStatement((QueryS2QLReq) queryReq, user);
            return getExplainResp(queryStatement);
        }
        if (QueryTypeEnum.STRUCT.equals(queryTypeEnum) && queryReq instanceof QueryStructReq) {
            QueryStatement queryStatement = semanticQueryEngine.plan((QueryStructReq) queryReq);
            return getExplainResp(queryStatement);
        }
        if (QueryTypeEnum.STRUCT.equals(queryTypeEnum) && queryReq instanceof QueryMultiStructReq) {
            QueryMultiStructReq queryMultiStructReq = (QueryMultiStructReq) queryReq;
            QueryStatement queryStatement = getQueryStatementByMultiStruct(queryMultiStructReq);
            return getExplainResp(queryStatement);
        }

        throw new IllegalArgumentException("Parameters are invalid, explainSqlReq: " + explainSqlReq);
    }

    private ExplainResp getExplainResp(QueryStatement queryStatement) {
        String sql = "";
        if (Objects.nonNull(queryStatement)) {
            sql = queryStatement.getSql();
        }
        return ExplainResp.builder().sql(sql).build();
    }


    public QueryStatement parseMetricReq(MetricReq metricReq) throws Exception {
        QueryStructReq queryStructCmd = new QueryStructReq();
        return semanticQueryEngine.physicalSql(queryStructCmd, metricReq);
    }

    private boolean isCache(QueryStructReq queryStructCmd) {
        if (!cacheEnable) {
            return false;
        }
        if (queryStructCmd.getCacheInfo() != null) {
            return queryStructCmd.getCacheInfo().getCache();
        }
        return false;
    }

    private boolean isCache(QueryMultiStructReq queryStructCmd) {
        if (!cacheEnable) {
            return false;
        }
        if (!CollectionUtils.isEmpty(queryStructCmd.getQueryStructReqs())
                && queryStructCmd.getQueryStructReqs().get(0).getCacheInfo() != null) {
            return queryStructCmd.getQueryStructReqs().get(0).getCacheInfo().getCache();
        }
        return false;
    }

    private QueryResultWithSchemaResp queryByCache(String key, Object queryCmd) {

        Object resultObject = cacheUtils.get(key);
        if (Objects.nonNull(resultObject)) {
            log.info("queryByStructWithCache, key:{}, queryCmd:{}", key, queryCmd.toString());
            statUtils.updateResultCacheKey(key);
            return (QueryResultWithSchemaResp) resultObject;
        }
        return null;
    }

    private QueryStructReq generateDimValueQueryStruct(QueryDimValueReq queryDimValueReq) {
        QueryStructReq queryStructReq = new QueryStructReq();

        queryStructReq.setModelId(queryDimValueReq.getModelId());
        queryStructReq.setGroups(Collections.singletonList(queryDimValueReq.getDimensionBizName()));

        if (!Objects.isNull(queryDimValueReq.getValue())) {
            List<Filter> dimensionFilters = new ArrayList<>();
            Filter dimensionFilter = new Filter();
            dimensionFilter.setOperator(FilterOperatorEnum.LIKE);
            dimensionFilter.setRelation(Filter.Relation.FILTER);
            dimensionFilter.setBizName(queryDimValueReq.getDimensionBizName());
            dimensionFilter.setValue(queryDimValueReq.getValue());
            dimensionFilters.add(dimensionFilter);
            queryStructReq.setDimensionFilters(dimensionFilters);
        }
        List<Aggregator> aggregators = new ArrayList<>();
        queryStructReq.setAggregators(aggregators);

        DateConf dateInfo = new DateConf();
        dateInfo.setDateMode(DateConf.DateMode.RECENT);
        dateInfo.setUnit(1);
        queryStructReq.setDateInfo(dateInfo);
        return queryStructReq;
    }


}
