package com.tencent.supersonic.semantic.query.utils;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.TaskStatusEnum;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import com.tencent.supersonic.semantic.api.model.enums.QueryTypeBackEnum;
import com.tencent.supersonic.semantic.api.model.enums.QueryTypeEnum;
import com.tencent.supersonic.semantic.api.model.pojo.QueryStat;
import com.tencent.supersonic.semantic.api.model.pojo.SchemaItem;
import com.tencent.supersonic.semantic.api.model.response.ModelSchemaResp;
import com.tencent.supersonic.semantic.api.query.request.ItemUseReq;
import com.tencent.supersonic.semantic.api.query.request.QueryS2QLReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.semantic.api.query.response.ItemUseResp;
import com.tencent.supersonic.semantic.model.domain.ModelService;
import com.tencent.supersonic.semantic.api.model.enums.QueryOptMode;

import com.tencent.supersonic.semantic.query.persistence.repository.StatRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;


@Component
@Slf4j
public class StatUtils {

    private static final TransmittableThreadLocal<QueryStat> STATS = new TransmittableThreadLocal<>();
    private final StatRepository statRepository;
    private final SqlFilterUtils sqlFilterUtils;

    private final ModelService modelService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StatUtils(StatRepository statRepository,
            SqlFilterUtils sqlFilterUtils,
            ModelService modelService) {

        this.statRepository = statRepository;
        this.sqlFilterUtils = sqlFilterUtils;
        this.modelService = modelService;
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
        log.info("queryStatInfo: {}", queryStatInfo);
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


    public void initStatInfo(QueryS2QLReq queryS2QLReq, User facadeUser) {
        QueryStat queryStatInfo = new QueryStat();
        List<String> allFields = SqlParserSelectHelper.getAllFields(queryS2QLReq.getSql());
        queryStatInfo.setModelId(queryS2QLReq.getModelId());
        ModelSchemaResp modelSchemaResp = modelService.fetchSingleModelSchema(queryS2QLReq.getModelId());

        List<String> dimensions = new ArrayList<>();
        if (Objects.nonNull(modelSchemaResp)) {
            dimensions = getFieldNames(allFields, modelSchemaResp.getDimensions());
        }

        List<String> metrics = new ArrayList<>();
        if (Objects.nonNull(modelSchemaResp)) {
            metrics = getFieldNames(allFields, modelSchemaResp.getMetrics());
        }

        String userName = getUserName(facadeUser);
        try {
            queryStatInfo.setTraceId("")
                    .setModelId(queryS2QLReq.getModelId())
                    .setUser(userName)
                    .setQueryType(QueryTypeEnum.SQL.getValue())
                    .setQueryTypeBack(QueryTypeBackEnum.NORMAL.getState())
                    .setQuerySqlCmd(queryS2QLReq.toString())
                    .setQuerySqlCmdMd5(DigestUtils.md5Hex(queryS2QLReq.toString()))
                    .setStartTime(System.currentTimeMillis())
                    .setUseResultCache(true)
                    .setUseSqlCache(true)
                    .setMetrics(objectMapper.writeValueAsString(metrics))
                    .setDimensions(objectMapper.writeValueAsString(dimensions));
        } catch (JsonProcessingException e) {
            log.error("initStatInfo:{}", e);
        }
        StatUtils.set(queryStatInfo);
    }



    public void initStatInfo(QueryStructReq queryStructCmd, User facadeUser) {
        QueryStat queryStatInfo = new QueryStat();
        String traceId = "";
        List<String> dimensions = queryStructCmd.getGroups();

        List<String> metrics = new ArrayList<>();
        queryStructCmd.getAggregators().stream().forEach(aggregator -> metrics.add(aggregator.getColumn()));
        String user = getUserName(facadeUser);

        try {
            queryStatInfo.setTraceId(traceId)
                    .setModelId(queryStructCmd.getModelId())
                    .setUser(user)
                    .setQueryType(QueryTypeEnum.STRUCT.getValue())
                    .setQueryTypeBack(QueryTypeBackEnum.NORMAL.getState())
                    .setQueryStructCmd(queryStructCmd.toString())
                    .setQueryStructCmdMd5(DigestUtils.md5Hex(queryStructCmd.toString()))
                    .setStartTime(System.currentTimeMillis())
                    .setNativeQuery(queryStructCmd.getNativeQuery())
                    .setGroupByCols(objectMapper.writeValueAsString(queryStructCmd.getGroups()))
                    .setAggCols(objectMapper.writeValueAsString(queryStructCmd.getAggregators()))
                    .setOrderByCols(objectMapper.writeValueAsString(queryStructCmd.getOrders()))
                    .setFilterCols(objectMapper.writeValueAsString(
                            sqlFilterUtils.getFiltersCol(queryStructCmd.getOriginalFilter())))
                    .setUseResultCache(true)
                    .setUseSqlCache(true)
                    .setMetrics(objectMapper.writeValueAsString(metrics))
                    .setDimensions(objectMapper.writeValueAsString(dimensions))
                    .setQueryOptMode(QueryOptMode.NONE.name());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
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

    public Boolean updateQueryOptMode(String mode) {
        STATS.get().setQueryOptMode(mode);
        return true;
    }

    public List<ItemUseResp> getStatInfo(ItemUseReq itemUseCommend) {
        return statRepository.getStatInfo(itemUseCommend);
    }

    public List<QueryStat> getQueryStatInfoWithoutCache(ItemUseReq itemUseCommend) {
        return statRepository.getQueryStatInfoWithoutCache(itemUseCommend);
    }
}