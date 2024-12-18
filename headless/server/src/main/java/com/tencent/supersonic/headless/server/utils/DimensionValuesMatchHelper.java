package com.tencent.supersonic.headless.server.utils;

import com.tencent.supersonic.common.pojo.DimValuesConstants;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.core.cache.QueryCache;
import com.tencent.supersonic.headless.core.pojo.Database;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.utils.ComponentFactory;
import com.tencent.supersonic.headless.core.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
public class DimensionValuesMatchHelper {

    private static final QueryCache queryCache = ComponentFactory.getQueryCache();

    static void dimensionValuesStoreToCache(ChatQueryContext chatQueryContext) {
        String queryId = String.valueOf(chatQueryContext.getRequest().getQueryId());
        queryCache.put(queryId + DimValuesConstants.CONDITION, true);
        queryCache.put(queryId + DimValuesConstants.DIMENSION_VALUS_AND_ID, chatQueryContext.getSchemaValusByTerm());
        log.info("这里记录了缓存： key : {}, value : {}", queryId + DimValuesConstants.DIMENSION_VALUS_AND_ID, true);
    }

    private static String buildQuery(List<Map.Entry<String, String>> dimensionValuesAndId) {
        // 初始化 SQL 查询
        StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT pid1_2022, pid2_2022, pid3_2022, pid4_2022 FROM supersonic.hhweb_user_active_scene_index_2022_d WHERE 1=1");

        // 定义需要的维度 ID
        Set<String> requiredDimensionIds = new HashSet<>(Arrays.asList("53", "54", "55", "56"));

        // 遍历 dimensionValuesAndId，根据 key 映射到字段并拼接条件
        for (Map.Entry<String, String> entry : dimensionValuesAndId) {
            if (requiredDimensionIds.contains(entry.getKey())) {
                String column = mapKeyToColumn(entry.getKey());
                String value = sanitizeSqlValue(entry.getValue());
                sql.append(" AND ").append(column).append(" = '").append(value).append("'");
            }
        }

        // 获取最近三天的日期范围
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String threeDaysAgo = LocalDate.now().minusDays(5).format(DateTimeFormatter.BASIC_ISO_DATE);

        // 添加 period_id 条件，最近三天
        sql.append(" AND period_id BETWEEN '").append(threeDaysAgo).append("' AND '").append(today).append("'");

        return sql.toString();
    }

    /**
     * 将维度 ID 映射到数据库字段名。
     */
    private static String mapKeyToColumn(String key) {
        switch (key) {
            case "53":
                return "pid1_2022";
            case "54":
                return "pid2_2022";
            case "55":
                return "pid3_2022";
            case "56":
                return "pid4_2022";
            case "58":
                return "user_type";
            case "57":
                return "province_name";
            case "52":
                return "client_type";
            case "51":
                return "company_name";
            case "50":
                return "city";
            case "59":
                return "channel_level1";
            case "60":
                return "channel_level2";
            default:
                throw new IllegalArgumentException("未知的维度 ID: " + key);
        }
    }

    /**
     * 对 SQL 值进行简单的安全处理，防止注入。
     */
    private static String sanitizeSqlValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        // 替换单引号为双单引号，防止 SQL 注入,进一步去除分号，防止语句拼接
        return value.replace("'", "''")
                .replace(";", "");
    }



    public static SemanticQueryResp executeQuery(List<Map.Entry<String, String>> dimensionValuesAndId, QueryStatement queryStatement) {

        SqlUtils sqlUtils = ContextUtils.getBean(SqlUtils.class);
        String sql = buildQuery(dimensionValuesAndId);
        log.info("executing SQL: {}", sql);
        Database database = queryStatement.getOntology().getDatabase();
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

