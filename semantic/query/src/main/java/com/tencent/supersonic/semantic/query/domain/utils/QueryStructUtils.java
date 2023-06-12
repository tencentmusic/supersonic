package com.tencent.supersonic.semantic.query.domain.utils;

import static com.tencent.supersonic.common.constant.Constants.UNIONALL;

import com.tencent.supersonic.semantic.api.core.pojo.ItemDateFilter;
import com.tencent.supersonic.semantic.api.core.response.DimensionResp;
import com.tencent.supersonic.semantic.api.core.response.ItemDateResp;
import com.tencent.supersonic.semantic.api.core.response.MetricResp;
import com.tencent.supersonic.semantic.api.core.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.SqlParserResp;
import com.tencent.supersonic.semantic.api.query.pojo.Cache;
import com.tencent.supersonic.semantic.api.query.request.QueryMultiStructReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.common.constant.Constants;
import com.tencent.supersonic.common.enums.TypeEnums;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.SchemaItem;
import com.tencent.supersonic.common.util.cache.CacheUtils;
import com.tencent.supersonic.semantic.core.domain.DatabaseService;
import com.tencent.supersonic.semantic.core.domain.DatasourceService;
import com.tencent.supersonic.semantic.core.domain.DimensionService;
import com.tencent.supersonic.semantic.core.domain.MetricService;
import com.tencent.supersonic.semantic.query.domain.ParserService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;


@Slf4j
@Component
public class QueryStructUtils {

    private final DatabaseService databaseService;
    private final QueryUtils queryUtils;
    private final ParserService parserService;
    private final SqlParserUtils sqlParserUtils;
    private final StatUtils statUtils;
    private final DimensionService dimensionService;
    private final MetricService metricService;
    private final DatasourceService datasourceService;
    private final com.tencent.supersonic.domain.semantic.query.domain.utils.DateUtils dateUtils;
    private final SqlFilterUtils sqlFilterUtils;
    private final CacheUtils cacheUtils;

    @Value("${query.cache.enable:true}")
    private Boolean cacheEnable;

    Set<String> internalCols = new HashSet<>(
            Arrays.asList("dayno", "plat_sys_var", "sys_imp_date", "sys_imp_week", "sys_imp_month"));

    public QueryStructUtils(DatabaseService databaseService,
            QueryUtils queryUtils,
            ParserService parserService,
            SqlParserUtils sqlParserUtils,
            StatUtils statUtils,
            DimensionService dimensionService,
            MetricService metricService,
            DatasourceService datasourceService,
            com.tencent.supersonic.domain.semantic.query.domain.utils.DateUtils dateUtils,
            SqlFilterUtils sqlFilterUtils,
            CacheUtils cacheUtils) {
        this.databaseService = databaseService;
        this.queryUtils = queryUtils;
        this.parserService = parserService;
        this.sqlParserUtils = sqlParserUtils;
        this.statUtils = statUtils;
        this.dimensionService = dimensionService;
        this.metricService = metricService;
        this.datasourceService = datasourceService;
        this.dateUtils = dateUtils;
        this.sqlFilterUtils = sqlFilterUtils;
        this.cacheUtils = cacheUtils;
    }


    public QueryResultWithSchemaResp queryByStructByCache(QueryStructReq queryStructCmd, String key) throws Exception {

        QueryResultWithSchemaResp queryResultWithColumns;
        Object resultObject = cacheUtils.get(key);
        if (Objects.nonNull(resultObject)) {
            log.info("queryByStructWithCache, key:{}, queryStructCmd:{}", key, queryStructCmd.toString());
            statUtils.updateResultCacheKey(key);
            return (QueryResultWithSchemaResp) resultObject;
        }

        // if cache data is null, query database
        queryResultWithColumns = queryByStructWithoutCache(queryStructCmd, key);
        return queryResultWithColumns;
    }

    public boolean queryCache(Cache cacheInfo) {
        if (Objects.isNull(cacheInfo)) {
            return true;
        }
        return Objects.nonNull(cacheInfo)
                && cacheInfo.getCache();
    }


    public QueryResultWithSchemaResp queryByStructWithoutCache(QueryStructReq queryStructCmd, String key)
            throws Exception {

        log.info("stat queryByStructWithoutCache, queryStructCmd:{}", queryStructCmd);
        StatUtils.get().setUseResultCache(false);
        SqlParserResp sqlParser = getSqlParser(queryStructCmd);
        queryUtils.checkSqlParse(sqlParser);
        log.info("sqlParser:{}", sqlParser);

        queryUtils.handleDetail(queryStructCmd, sqlParser);
        QueryResultWithSchemaResp queryResultWithColumns = databaseService.queryWithColumns(sqlParser);

        queryUtils.fillItemNameInfo(queryResultWithColumns, queryStructCmd.getDomainId());
        queryResultWithColumns.setSql(sqlParser.getSql());

        // if queryResultWithColumns is not null, update cache data
        cacheResultLogic(key, queryResultWithColumns);

        return queryResultWithColumns;
    }

    private void cacheResultLogic(String key, QueryResultWithSchemaResp queryResultWithColumns) {
        if (cacheEnable && Objects.nonNull(queryResultWithColumns) && !CollectionUtils.isEmpty(
                queryResultWithColumns.getResultList())) {
            QueryResultWithSchemaResp finalQueryResultWithColumns = queryResultWithColumns;
            CompletableFuture.supplyAsync(() -> cacheUtils.put(key, finalQueryResultWithColumns))
                    .exceptionally(exception -> {
                        log.warn("exception:", exception);
                        return null;
                    });
            statUtils.updateResultCacheKey(key);
            log.info("add record to cache, key:{}", key);
        }

    }

    public QueryResultWithSchemaResp queryByMultiStructWithoutCache(QueryMultiStructReq queryMultiStructCmd, String key)
            throws Exception {

        log.info("stat queryByStructWithoutCache, queryMultiStructCmd:{}", queryMultiStructCmd);
        QueryResultWithSchemaResp queryResultWithColumns;

        List<SqlParserResp> sqlParsers = new ArrayList<>();
        for (QueryStructReq queryStructCmd : queryMultiStructCmd.getQueryStructCmds()) {
            SqlParserResp sqlParser = getSqlParser(queryStructCmd);
            queryUtils.checkSqlParse(sqlParser);
            queryUtils.handleDetail(queryStructCmd, sqlParser);
            sqlParsers.add(sqlParser);
        }
        log.info("multi sqlParser:{}", sqlParsers);

        SqlParserResp sqlParser = sqlParserUnion(queryMultiStructCmd, sqlParsers);
        queryResultWithColumns = databaseService.queryWithColumns(sqlParser);
        queryUtils.fillItemNameInfo(queryResultWithColumns, queryMultiStructCmd);
        queryResultWithColumns.setSql(sqlParser.getSql());

        cacheResultLogic(key, queryResultWithColumns);
        return queryResultWithColumns;
    }

    private SqlParserResp sqlParserUnion(QueryMultiStructReq queryMultiStructCmd, List<SqlParserResp> sqlParsers) {
        SqlParserResp sqlParser = new SqlParserResp();
        StringBuilder unionSqlBuilder = new StringBuilder();
        for (int i = 0; i < sqlParsers.size(); i++) {
            String selectStr = SqlGenerateUtils.getUnionSelect(queryMultiStructCmd.getQueryStructCmds().get(i));
            unionSqlBuilder.append(String.format("select %s from ( %s ) sub_sql_%s",
                    selectStr,
                    sqlParsers.get(i).getSql(), i));
            unionSqlBuilder.append(UNIONALL);
        }
        String unionSql = unionSqlBuilder.substring(0, unionSqlBuilder.length() - Constants.UNIONALL.length());
        sqlParser.setSql(unionSql);
        sqlParser.setSourceId(sqlParsers.get(0).getSourceId());
        log.info("union sql parser:{}", sqlParser);
        return sqlParser;
    }


    private SqlParserResp getSqlParser(QueryStructReq queryStructCmd) throws Exception {
        return sqlParserUtils.getSqlParserWithoutCache(queryStructCmd);
    }

    private List<Long> getDimensionIds(QueryStructReq queryStructCmd) {
        List<Long> dimensionIds = new ArrayList<>();
        List<DimensionResp> dimensions = dimensionService.getDimensions(queryStructCmd.getDomainId());
        Map<String, List<DimensionResp>> pair = dimensions.stream()
                .collect(Collectors.groupingBy(DimensionResp::getBizName));
        for (String group : queryStructCmd.getGroups()) {
            if (pair.containsKey(group)) {
                dimensionIds.add(pair.get(group).get(0).getId());
            }
        }

        List<String> filtersCols = sqlFilterUtils.getFiltersCol(queryStructCmd.getOriginalFilter());
        for (String col : filtersCols) {
            if (pair.containsKey(col)) {
                dimensionIds.add(pair.get(col).get(0).getId());
            }
        }

        return dimensionIds;
    }

    private List<Long> getMetricIds(QueryStructReq queryStructCmd) {
        List<Long> metricIds = new ArrayList<>();
        List<MetricResp> metrics = metricService.getMetrics(queryStructCmd.getDomainId());
        Map<String, List<MetricResp>> pair = metrics.stream().collect(Collectors.groupingBy(SchemaItem::getBizName));
        for (Aggregator agg : queryStructCmd.getAggregators()) {
            if (pair.containsKey(agg.getColumn())) {
                metricIds.add(pair.get(agg.getColumn()).get(0).getId());
            }
        }
        List<String> filtersCols = sqlFilterUtils.getFiltersCol(queryStructCmd.getOriginalFilter());
        for (String col : filtersCols) {
            if (pair.containsKey(col)) {
                metricIds.add(pair.get(col).get(0).getId());
            }
        }
        return metricIds;
    }

    public String getDateWhereClause(QueryStructReq queryStructCmd) {
        DateConf dateInfo = queryStructCmd.getDateInfo();
        if (Objects.isNull(dateInfo) || Objects.isNull(dateInfo.getDateMode())) {
            return "";
        }
        List<Long> dimensionIds = getDimensionIds(queryStructCmd);
        List<Long> metricIds = getMetricIds(queryStructCmd);

        ItemDateResp dateDate = datasourceService.getDateDate(
                new ItemDateFilter(dimensionIds, TypeEnums.DIMENSION.getName()),
                new ItemDateFilter(metricIds, TypeEnums.METRIC.getName()));
        if (Objects.isNull(dateDate)
                || Strings.isEmpty(dateDate.getStartDate())
                && Strings.isEmpty(dateDate.getEndDate())) {
            if (dateUtils.hasDataMode(dateInfo)) {
                return dateUtils.hasDataModeStr(dateDate, dateInfo);
            }
            return dateUtils.defaultRecentDateInfo(queryStructCmd.getDateInfo());
        }
        log.info("dateDate:{}", dateDate);
        return dateUtils.getDateWhereStr(dateInfo, dateDate);
    }


    public String generateWhere(QueryStructReq queryStructCmd) {
        String whereClauseFromFilter = sqlFilterUtils.getWhereClause(queryStructCmd.getOriginalFilter());
        String whereFromDate = getDateWhereClause(queryStructCmd);
        if (Strings.isNotEmpty(whereFromDate) && Strings.isNotEmpty(whereClauseFromFilter)) {
            return String.format("%s AND (%s)", whereFromDate, whereClauseFromFilter);
        } else if (Strings.isEmpty(whereFromDate) && Strings.isNotEmpty(whereClauseFromFilter)) {
            return whereClauseFromFilter;
        } else if (Strings.isNotEmpty(whereFromDate) && Strings.isEmpty(whereClauseFromFilter)) {
            return whereFromDate;
        } else if (Strings.isEmpty(whereFromDate) && Strings.isEmpty(whereClauseFromFilter)) {
            log.info("the current date information is empty, enter the date initialization logic");
            return dateUtils.defaultRecentDateInfo(queryStructCmd.getDateInfo());
        }
        return whereClauseFromFilter;
    }

    public Set<String> getResNameEn(QueryStructReq queryStructCmd) {
        Set<String> resNameEnSet = new HashSet<>();
        queryStructCmd.getAggregators().stream().forEach(agg -> resNameEnSet.add(agg.getColumn()));
        resNameEnSet.addAll(queryStructCmd.getGroups());
        queryStructCmd.getOrders().stream().forEach(order -> resNameEnSet.add(order.getColumn()));
        sqlFilterUtils.getFiltersCol(queryStructCmd.getOriginalFilter()).stream().forEach(col -> resNameEnSet.add(col));
        return resNameEnSet;
    }

    public Set<String> getResNameEnExceptInternalCol(QueryStructReq queryStructCmd) {
        Set<String> resNameEnSet = getResNameEn(queryStructCmd);
        return resNameEnSet.stream().filter(res -> !internalCols.contains(res)).collect(Collectors.toSet());
    }

    public Set<String> getFilterResNameEn(QueryStructReq queryStructCmd) {
        Set<String> resNameEnSet = new HashSet<>();
        sqlFilterUtils.getFiltersCol(queryStructCmd.getOriginalFilter()).stream().forEach(col -> resNameEnSet.add(col));
        return resNameEnSet;
    }

    public Set<String> getFilterResNameEnExceptInternalCol(QueryStructReq queryStructCmd) {
        Set<String> resNameEnSet = getFilterResNameEn(queryStructCmd);
        return resNameEnSet.stream().filter(res -> !internalCols.contains(res)).collect(Collectors.toSet());
    }

}

