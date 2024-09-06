package com.tencent.supersonic.headless.api.pojo;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.ToString;

import java.util.List;
import java.util.Objects;

@Data
@ToString
public class QueryStat {

    private Long id;
    private String traceId;
    private Long modelId;

    private Long dataSetId;
    private String user;
    private String createdAt;
    /** corresponding type, such as sql, struct, etc */
    private String queryType;
    /** NORMAL, PRE_FLUSH */
    private Integer queryTypeBack;

    private String querySqlCmd;
    private String querySqlCmdMd5;
    private String queryStructCmd;
    private String queryStructCmdMd5;
    private String sql;
    private String sqlMd5;
    private String queryEngine;
    private Long startTime;
    private Long elapsedMs;
    private String queryState;
    private Boolean nativeQuery;
    private String startDate;
    private String endDate;
    private String dimensions;
    private String metrics;
    private String selectCols;
    private String aggCols;
    private String filterCols;
    private String groupByCols;
    private String orderByCols;
    private Boolean useResultCache;
    private Boolean useSqlCache;
    private String sqlCacheKey;
    private String resultCacheKey;
    private String queryOptMode;

    public QueryStat setQueryOptMode(String queryOptMode) {
        this.queryOptMode = queryOptMode;
        return this;
    }

    public QueryStat setQuerySqlCmdMd5(String querySqlCmdMd5) {
        this.querySqlCmdMd5 = querySqlCmdMd5;
        return this;
    }

    public QueryStat setQueryStructCmdMd5(String queryStructCmdMd5) {
        this.queryStructCmdMd5 = queryStructCmdMd5;
        return this;
    }

    public QueryStat setStartTime(Long startTime) {
        this.startTime = startTime;
        return this;
    }

    public QueryStat setQueryTypeBack(Integer queryTypeBack) {
        this.queryTypeBack = queryTypeBack;
        return this;
    }

    public QueryStat setNativeQuery(Boolean nativeQuery) {
        this.nativeQuery = nativeQuery;
        return this;
    }

    public QueryStat setTraceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    public QueryStat setModelId(Long modelId) {
        this.modelId = modelId;
        return this;
    }

    public QueryStat setDataSetId(Long dataSetId) {
        this.dataSetId = dataSetId;
        return this;
    }

    public QueryStat setUser(String user) {
        this.user = user;
        return this;
    }

    public QueryStat setQueryType(String queryType) {
        this.queryType = queryType;
        return this;
    }

    public QueryStat setQuerySqlCmd(String querySqlCmd) {
        this.querySqlCmd = querySqlCmd;
        return this;
    }

    public QueryStat setQueryStructCmd(String queryStructCmd) {
        this.queryStructCmd = queryStructCmd;
        return this;
    }

    public QueryStat setSql(String sql) {
        this.sql = sql;
        return this;
    }

    public QueryStat setSqlMd5(String sqlMd5) {
        this.sqlMd5 = sqlMd5;
        return this;
    }

    public QueryStat setQueryEngine(String queryEngine) {
        this.queryEngine = queryEngine;
        return this;
    }

    public QueryStat setElapsedMs(Long elapsedMs) {
        this.elapsedMs = elapsedMs;
        return this;
    }

    public QueryStat setQueryState(String queryState) {
        this.queryState = queryState;
        return this;
    }

    public QueryStat setStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    public QueryStat setEndDate(String endDate) {
        this.endDate = endDate;
        return this;
    }

    public QueryStat setDimensions(String dimensions) {
        this.dimensions = dimensions;
        return this;
    }

    public QueryStat setMetrics(String metrics) {
        this.metrics = metrics;
        return this;
    }

    public QueryStat setSelectCols(String selectCols) {
        this.selectCols = selectCols;
        return this;
    }

    public QueryStat setAggCols(String aggCols) {
        this.aggCols = aggCols;
        return this;
    }

    public QueryStat setFilterCols(String filterCols) {
        this.filterCols = filterCols;
        return this;
    }

    public QueryStat setGroupByCols(String groupByCols) {
        this.groupByCols = groupByCols;
        return this;
    }

    public QueryStat setOrderByCols(String orderByCols) {
        this.orderByCols = orderByCols;
        return this;
    }

    public QueryStat setUseResultCache(Boolean useResultCache) {
        this.useResultCache = useResultCache;
        return this;
    }

    public QueryStat setUseSqlCache(Boolean useSqlCache) {
        this.useSqlCache = useSqlCache;
        return this;
    }

    public QueryStat setSqlCacheKey(String sqlCacheKey) {
        this.sqlCacheKey = sqlCacheKey;
        return this;
    }

    public QueryStat setResultCacheKey(String resultCacheKey) {
        this.resultCacheKey = resultCacheKey;
        return this;
    }

    public QueryStat setId(Long id) {
        this.id = id;
        return this;
    }

    public QueryStat setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public List<String> getMetricListBizName() {
        if (Objects.isNull(metrics)) {
            return Lists.newArrayList();
        }
        return JSONObject.parseArray(metrics, String.class);
    }

    public List<String> getDimensionListBizName() {
        return JSONObject.parseArray(dimensions, String.class);
    }
}
