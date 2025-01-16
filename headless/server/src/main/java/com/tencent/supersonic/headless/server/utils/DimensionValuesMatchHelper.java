package com.tencent.supersonic.headless.server.utils;

import com.tencent.supersonic.common.pojo.DimValuesConstants;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.DimensionMappingConfig;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.core.cache.QueryCache;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.utils.ComponentFactory;
import com.tencent.supersonic.headless.core.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
public class DimensionValuesMatchHelper {

    private final QueryCache queryCache = ComponentFactory.getQueryCache();

    @Autowired
    private DimensionMappingConfig dimensionMappingConfig;

    void dimensionValuesStoreToCache(ChatQueryContext chatQueryContext) {
        String queryId = String.valueOf(chatQueryContext.getRequest().getQueryId());
        queryCache.put(queryId + DimValuesConstants.CONDITION, true);
        queryCache.put(queryId + DimValuesConstants.DIMENSION_VALUS_AND_ID,
                chatQueryContext.getSchemaValusByTerm());
        log.info("这里记录了缓存： key : {}, value : {}",
                queryId + DimValuesConstants.DIMENSION_VALUS_AND_ID, true);
    }

    private String buildQuery(List<Map.Entry<String, String>> dimensionValuesAndId) {
        // 从配置中获取数据库名称
        String databaseName = dimensionMappingConfig.getDatabaseName();

        // 初始化 SQL 查询
        StringBuilder sql = new StringBuilder(String.format(
                "SELECT DISTINCT pid1_2022, pid2_2022, pid3_2022, pid4_2022 FROM %s.hhweb_user_active_scene_index_2022_d WHERE 1=1",
                databaseName));
        Map<String, String> mapping = dimensionMappingConfig.getMapping();

        for (Map.Entry<String, String> entry : dimensionValuesAndId) {
            if (mapping.containsValue(entry.getKey())) {
                String column = mapping.entrySet().stream()
                        .filter(e -> e.getValue().equals(entry.getKey())).map(Map.Entry::getKey)
                        .findFirst().orElseThrow(
                                () -> new IllegalArgumentException("未知的维度 ID: " + entry.getKey()));
                String value = sanitizeSqlValue(entry.getValue());
                sql.append(" AND ").append(column).append(" = '").append(value).append("'");
            }
        }

        // 获取最近3天的日期范围
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String threeDaysAgo = LocalDate.now().minusDays(3).format(DateTimeFormatter.BASIC_ISO_DATE);

        // 添加 period_id 条件，最近3天
        sql.append(" AND period_id BETWEEN '").append(threeDaysAgo).append("' AND '").append(today)
                .append("'");

        // 添加 LIMIT 子句，限制结果为前 1000 条
        sql.append(" LIMIT 1000");
        return sql.toString();
    }

    /**
     * 对 SQL 值进行简单的安全处理，防止注入。
     */
    private String sanitizeSqlValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        return value.replace("'", "''").replace(";", "");
    }



    public SemanticQueryResp executeQuery(List<Map.Entry<String, String>> dimensionValuesAndId,
            QueryStatement queryStatement) {

        SqlUtils sqlUtils = ContextUtils.getBean(SqlUtils.class);
        String sql = buildQuery(dimensionValuesAndId);
        log.info("executing SQL: {}", sql);
        DatabaseResp database = queryStatement.getOntology().getDatabase();
        SemanticQueryResp queryResultWithColumns = new SemanticQueryResp();
        try {
            SqlUtils sqlUtil = sqlUtils.init(database);
            sqlUtil.queryInternal(sql, queryResultWithColumns);
            queryResultWithColumns.setSql(sql);
        } catch (Exception e) {
            log.error("queryInternal error [{}]", StringUtils.normalizeSpace(e.toString()));
            queryResultWithColumns.setErrorMsg(e.getMessage());
        }
        return queryResultWithColumns;
    }
}
