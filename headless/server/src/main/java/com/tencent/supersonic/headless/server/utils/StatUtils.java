package com.tencent.supersonic.headless.server.utils;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.TaskStatusEnum;
import com.tencent.supersonic.common.util.SqlFilterUtils;
import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.headless.api.pojo.QueryStat;
import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import com.tencent.supersonic.headless.api.pojo.enums.QueryOptMode;
import com.tencent.supersonic.headless.api.pojo.enums.QueryMethod;
import com.tencent.supersonic.headless.api.pojo.enums.QueryTypeBack;
import com.tencent.supersonic.headless.api.pojo.request.ItemUseReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryMultiStructReq;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryTagReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.ItemUseResp;
import com.tencent.supersonic.headless.server.persistence.repository.StatRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@Component
@Slf4j
public class StatUtils {

    private static final TransmittableThreadLocal<QueryStat> STATS = new TransmittableThreadLocal<>();
    private final StatRepository statRepository;
    private final SqlFilterUtils sqlFilterUtils;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StatUtils(StatRepository statRepository,
            SqlFilterUtils sqlFilterUtils) {

        this.statRepository = statRepository;
        this.sqlFilterUtils = sqlFilterUtils;
    }

    public static QueryStat get() {
        return STATS.get();
    }

    public static void set(QueryStat queryStatInfo) {
        STATS.set(queryStatInfo);
    }

    public static void remove() {
        STATS.remove();
    }

    public void statInfo2DbAsync(TaskStatusEnum state) {
        QueryStat queryStatInfo = get();
        queryStatInfo.setElapsedMs(System.currentTimeMillis() - queryStatInfo.getStartTime());
        queryStatInfo.setQueryState(state.getStatus());
        CompletableFuture.runAsync(() -> {
            statRepository.createRecord(queryStatInfo);
        }).exceptionally(exception -> {
            log.warn("queryStatInfo, exception:", exception);
            return null;
        });

        remove();
    }

    public Boolean updateResultCacheKey(String key) {
        STATS.get().setResultCacheKey(key);
        return true;
    }

    public void initStatInfo(SemanticQueryReq semanticQueryReq, User facadeUser) {
        if (semanticQueryReq instanceof QuerySqlReq) {
            initSqlStatInfo((QuerySqlReq) semanticQueryReq, facadeUser);
        }
        if (semanticQueryReq instanceof QueryStructReq) {
            initStructStatInfo((QueryStructReq) semanticQueryReq, facadeUser);
        }
        if (semanticQueryReq instanceof QueryMultiStructReq) {
            QueryStructReq queryStructCmd = ((QueryMultiStructReq) semanticQueryReq).getQueryStructReqs().get(0);
            initStructStatInfo(queryStructCmd, facadeUser);
        }
        if (semanticQueryReq instanceof QueryTagReq) {
            initTagStatInfo((QueryTagReq) semanticQueryReq, facadeUser);
        }
    }

    public void initTagStatInfo(QueryTagReq queryTagReq, User facadeUser) {
        QueryStat queryStatInfo = new QueryStat();
        String traceId = "";
        List<String> dimensions = queryTagReq.getGroups();

        List<String> metrics = new ArrayList<>();
        queryTagReq.getAggregators().stream().forEach(aggregator -> metrics.add(aggregator.getColumn()));
        String user = getUserName(facadeUser);

        try {
            queryStatInfo.setTraceId(traceId)
                    .setDataSetId(queryTagReq.getDataSetId())
                    .setUser(user)
                    .setQueryType(QueryMethod.STRUCT.getValue())
                    .setQueryTypeBack(QueryTypeBack.NORMAL.getState())
                    .setQueryStructCmd(queryTagReq.toString())
                    .setQueryStructCmdMd5(DigestUtils.md5Hex(queryTagReq.toString()))
                    .setStartTime(System.currentTimeMillis())
                    .setNativeQuery(CollectionUtils.isEmpty(queryTagReq.getAggregators()))
                    .setGroupByCols(objectMapper.writeValueAsString(queryTagReq.getGroups()))
                    .setAggCols(objectMapper.writeValueAsString(queryTagReq.getAggregators()))
                    .setOrderByCols(objectMapper.writeValueAsString(queryTagReq.getOrders()))
                    .setFilterCols(objectMapper.writeValueAsString(
                            sqlFilterUtils.getFiltersCol(queryTagReq.getTagFilters())))
                    .setUseResultCache(true)
                    .setUseSqlCache(true)
                    .setMetrics(objectMapper.writeValueAsString(metrics))
                    .setDimensions(objectMapper.writeValueAsString(dimensions))
                    .setQueryOptMode(QueryOptMode.NONE.name());
            if (!CollectionUtils.isEmpty(queryTagReq.getModelIds())) {
                queryStatInfo.setModelId(queryTagReq.getModelIds().get(0));
            }
        } catch (JsonProcessingException e) {
            log.error("", e);
        }
        StatUtils.set(queryStatInfo);

    }

    public void initSqlStatInfo(QuerySqlReq querySqlReq, User facadeUser) {
        QueryStat queryStatInfo = new QueryStat();
        List<String> aggFields = SqlSelectHelper.getAggregateFields(querySqlReq.getSql());
        List<String> allFields = SqlSelectHelper.getAllFields(querySqlReq.getSql());
        List<String> dimensions = allFields.stream().filter(aggFields::contains).collect(Collectors.toList());

        String userName = getUserName(facadeUser);
        try {
            queryStatInfo.setTraceId("")
                    .setUser(userName)
                    .setDataSetId(querySqlReq.getDataSetId())
                    .setQueryType(QueryMethod.SQL.getValue())
                    .setQueryTypeBack(QueryTypeBack.NORMAL.getState())
                    .setQuerySqlCmd(querySqlReq.toString())
                    .setQuerySqlCmdMd5(DigestUtils.md5Hex(querySqlReq.toString()))
                    .setStartTime(System.currentTimeMillis())
                    .setUseResultCache(true)
                    .setUseSqlCache(true)
                    .setMetrics(objectMapper.writeValueAsString(aggFields))
                    .setDimensions(objectMapper.writeValueAsString(dimensions));
            if (!CollectionUtils.isEmpty(querySqlReq.getModelIds())) {
                queryStatInfo.setModelId(querySqlReq.getModelIds().get(0));
            }
        } catch (JsonProcessingException e) {
            log.error("initStatInfo:{}", e);
        }
        StatUtils.set(queryStatInfo);
    }

    public void initStructStatInfo(QueryStructReq queryStructReq, User facadeUser) {
        QueryStat queryStatInfo = new QueryStat();
        String traceId = "";
        List<String> dimensions = queryStructReq.getGroups();

        List<String> metrics = new ArrayList<>();
        queryStructReq.getAggregators().stream().forEach(aggregator -> metrics.add(aggregator.getColumn()));
        String user = getUserName(facadeUser);

        try {
            queryStatInfo.setTraceId(traceId)
                    .setDataSetId(queryStructReq.getDataSetId())
                    .setUser(user)
                    .setQueryType(QueryMethod.STRUCT.getValue())
                    .setQueryTypeBack(QueryTypeBack.NORMAL.getState())
                    .setQueryStructCmd(queryStructReq.toString())
                    .setQueryStructCmdMd5(DigestUtils.md5Hex(queryStructReq.toString()))
                    .setStartTime(System.currentTimeMillis())
                    .setNativeQuery(queryStructReq.getQueryType().isNativeAggQuery())
                    .setGroupByCols(objectMapper.writeValueAsString(queryStructReq.getGroups()))
                    .setAggCols(objectMapper.writeValueAsString(queryStructReq.getAggregators()))
                    .setOrderByCols(objectMapper.writeValueAsString(queryStructReq.getOrders()))
                    .setFilterCols(objectMapper.writeValueAsString(
                            sqlFilterUtils.getFiltersCol(queryStructReq.getOriginalFilter())))
                    .setUseResultCache(true)
                    .setUseSqlCache(true)
                    .setMetrics(objectMapper.writeValueAsString(metrics))
                    .setDimensions(objectMapper.writeValueAsString(dimensions))
                    .setQueryOptMode(QueryOptMode.NONE.name());
            if (!CollectionUtils.isEmpty(queryStructReq.getModelIds())) {
                queryStatInfo.setModelId(queryStructReq.getModelIds().get(0));
            }
        } catch (JsonProcessingException e) {
            log.error("", e);
        }
        StatUtils.set(queryStatInfo);

    }

    private List<String> getFieldNames(List<String> allFields, List<? extends SchemaItem> schemaItems) {
        Set<String> fieldNames = schemaItems
                .stream()
                .map(dimSchemaResp -> dimSchemaResp.getBizName())
                .collect(Collectors.toSet());
        if (!CollectionUtils.isEmpty(fieldNames)) {
            return allFields.stream().filter(fieldName -> fieldNames.contains(fieldName))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private String getUserName(User facadeUser) {
        return (Objects.nonNull(facadeUser) && Strings.isNotEmpty(facadeUser.getName())) ? facadeUser.getName()
                : "Admin";
    }

    public List<ItemUseResp> getStatInfo(ItemUseReq itemUseCommend) {
        return statRepository.getStatInfo(itemUseCommend);
    }
}
