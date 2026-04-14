package com.tencent.supersonic.headless.server.service.impl;

import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.SqlTemplateConfig;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.DataSetResp;
import com.tencent.supersonic.headless.core.utils.SqlTemplateEngine;
import com.tencent.supersonic.headless.server.service.DataSetService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Parses a serialized queryConfig string into the appropriate {@link SemanticQueryReq} subtype.
 *
 * <p>
 * Supports three config shapes:
 * <ol>
 * <li>SqlTemplateConfig – ST4 template with variable placeholders; rendered to QuerySqlReq.
 * <li>QueryStructReq – structured query (metrics, dimensions, filters, date range).
 * <li>QuerySqlReq – raw SQL (allowed only for report schedules, not for alert rules).
 * </ol>
 */
@Service
@Slf4j
public class QueryConfigParser {

    @Autowired(required = false)
    private SqlTemplateEngine sqlTemplateEngine;

    @Autowired(required = false)
    private DataSetService dataSetService;

    /**
     * Parse a queryConfig string for use in report schedules. Allows all three config types:
     * SqlTemplateConfig, QueryStructReq, and QuerySqlReq.
     *
     * @param queryConfig serialized JSON of the query config
     * @param datasetId dataset ID to inject when the config does not carry one
     * @param resolvedParams template parameters already resolved for the current execution
     * @return the parsed {@link SemanticQueryReq}
     */
    public SemanticQueryReq parse(String queryConfig, Long datasetId,
            Map<String, Object> resolvedParams) {
        if (StringUtils.isBlank(queryConfig)) {
            throw new IllegalArgumentException("queryConfig is required");
        }

        // Path 1: Try SqlTemplateConfig (ST4 template with variable rendering)
        try {
            SqlTemplateConfig templateConfig =
                    JsonUtil.toObject(queryConfig, SqlTemplateConfig.class);
            if (templateConfig != null && StringUtils.isNotBlank(templateConfig.getTemplateSql())) {
                if (sqlTemplateEngine == null) {
                    throw new IllegalStateException(
                            "SqlTemplateEngine is not available but queryConfig contains a SQL template");
                }
                Map<String, Object> params = resolvedParams != null ? resolvedParams : Map.of();
                String renderedSql =
                        sqlTemplateEngine.render(templateConfig.getTemplateSql(), params);
                QuerySqlReq sqlReq = new QuerySqlReq();
                sqlReq.setSql(renderedSql);
                sqlReq.setDataSetId(datasetId);
                return sqlReq;
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.debug("Failed to parse as SqlTemplateConfig, trying QueryStructReq");
        }

        // Path 2: Try QueryStructReq (structured query)
        // Return as-is instead of calling convert() — the StructQuery translator path
        // handles dateInfo properly via SqlGenerateUtils.getDateWhereClause() and also
        // generates correct SELECT columns from the dataset schema.
        try {
            QueryStructReq structReq = JsonUtil.toObject(queryConfig, QueryStructReq.class);
            if (structReq != null) {
                if (structReq.getDataSetId() == null && datasetId != null) {
                    structReq.setDataSetId(datasetId);
                }
                if (structReq.getDataSetId() != null) {
                    return structReq;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse as QueryStructReq, trying QuerySqlReq");
        }

        // Path 3: Try QuerySqlReq (raw SQL)
        try {
            QuerySqlReq sqlReq = JsonUtil.toObject(queryConfig, QuerySqlReq.class);
            if (sqlReq != null) {
                if (sqlReq.getDataSetId() == null) {
                    sqlReq.setDataSetId(datasetId);
                }
                return sqlReq;
            }
        } catch (Exception e) {
            log.debug("Failed to parse as QuerySqlReq");
        }

        throw new IllegalArgumentException("Unable to parse queryConfig: " + queryConfig);
    }

    /**
     * Parse a queryConfig string for use in alert rules.
     *
     * <p>
     * AG-06: Raw {@link QuerySqlReq} is not allowed in alert rules because it permits uncontrolled
     * user-injected SQL that can bypass row-count limits and security controls. Use
     * {@link QueryStructReq} or {@link SqlTemplateConfig} instead.
     *
     * <p>
     * AG-08: When the config resolves to a {@link QueryStructReq}, the limit is capped using the
     * dataset's query configuration to prevent runaway result sets during alert evaluation.
     *
     * @param queryConfig serialized JSON of the query config
     * @param datasetId dataset ID to inject when the config does not carry one
     * @return the parsed {@link SemanticQueryReq}
     */
    public SemanticQueryReq parseForAlert(String queryConfig, Long datasetId) {
        if (StringUtils.isBlank(queryConfig)) {
            throw new IllegalArgumentException("queryConfig is required");
        }

        // Path 1: Try SqlTemplateConfig (template-controlled SQL — allowed for alerts)
        try {
            SqlTemplateConfig templateConfig =
                    JsonUtil.toObject(queryConfig, SqlTemplateConfig.class);
            if (templateConfig != null && StringUtils.isNotBlank(templateConfig.getTemplateSql())) {
                if (sqlTemplateEngine == null) {
                    throw new IllegalStateException(
                            "SqlTemplateEngine is not available but queryConfig contains a SQL template");
                }
                // Render with empty params — alert rules have no runtime param context
                String renderedSql =
                        sqlTemplateEngine.render(templateConfig.getTemplateSql(), Map.of());
                // AG-08: enforce the dataset's detail limit by wrapping in a subquery if no LIMIT
                // is present
                String limitedSql =
                        injectAlertLimit(renderedSql, getAlertLimit(datasetId, QueryType.DETAIL));
                QuerySqlReq sqlReq = new QuerySqlReq();
                sqlReq.setSql(limitedSql);
                sqlReq.setDataSetId(datasetId);
                return sqlReq;
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.debug("Failed to parse as SqlTemplateConfig, trying QueryStructReq");
        }

        // Path 2: Try QueryStructReq (structured query — allowed and preferred for alerts)
        try {
            QueryStructReq structReq = JsonUtil.toObject(queryConfig, QueryStructReq.class);
            if (structReq != null) {
                // Inject datasetId from AlertRuleDO when the serialized config does not carry one
                if (structReq.getDataSetId() == null && datasetId != null) {
                    structReq.setDataSetId(datasetId);
                }
                if (structReq.getDataSetId() != null) {
                    // AG-08: cap limit using the dataset's query configuration
                    long currentLimit = structReq.getLimit();
                    long alertLimit =
                            getAlertLimit(structReq.getDataSetId(), structReq.getQueryType());
                    if (currentLimit <= 0 || currentLimit > alertLimit) {
                        log.info("AG-08: Capping alert query limit from {} to {} for dataset={}",
                                currentLimit, alertLimit, structReq.getDataSetId());
                        structReq.setLimit(alertLimit);
                    }
                    return structReq.convert(true);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse as QueryStructReq, checking for forbidden QuerySqlReq");
        }

        // Path 3: Raw QuerySqlReq — AG-06: forbidden in alert rules
        try {
            QuerySqlReq sqlReq = JsonUtil.toObject(queryConfig, QuerySqlReq.class);
            if (sqlReq != null) {
                throw new IllegalArgumentException(
                        "AG-06: QuerySqlReq is not allowed in alert rules. "
                                + "Use QueryStructReq or SqlTemplateConfig.");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.debug("Failed to parse as QuerySqlReq");
        }

        throw new IllegalArgumentException("Unable to parse queryConfig: " + queryConfig);
    }

    /**
     * AG-08: Wrap the rendered SQL in a limited subquery if it does not already contain a
     * {@code LIMIT} clause. This prevents runaway result sets during alert evaluation for
     * SqlTemplateConfig-based rules.
     */
    private String injectAlertLimit(String sql, long alertLimit) {
        // Simple heuristic: look for a standalone LIMIT keyword (case-insensitive)
        if (sql.toUpperCase().matches("(?s).*\\bLIMIT\\s+\\d+.*")) {
            log.debug("AG-08: SQL already contains LIMIT clause, skipping injection");
            return sql;
        }
        log.info("AG-08: Injecting LIMIT {} subquery wrapper for alert SQL", alertLimit);
        return "SELECT * FROM (" + sql + ") AS _alert_query_limit LIMIT " + alertLimit;
    }

    private long getAlertLimit(Long datasetId, QueryType queryType) {
        if (dataSetService == null || datasetId == null) {
            return getDefaultLimit(queryType);
        }
        try {
            DataSetResp dataSet = dataSetService.getDataSet(datasetId);
            if (dataSet == null || dataSet.getQueryConfig() == null) {
                return getDefaultLimit(queryType);
            }
            if (QueryType.AGGREGATE.equals(queryType)
                    && dataSet.getQueryConfig().getAggregateTypeDefaultConfig() != null) {
                long limit = dataSet.getQueryConfig().getAggregateTypeDefaultConfig().getLimit();
                return limit > 0 ? limit : Constants.DEFAULT_METRIC_LIMIT;
            }
            if (dataSet.getQueryConfig().getDetailTypeDefaultConfig() != null) {
                long limit = dataSet.getQueryConfig().getDetailTypeDefaultConfig().getLimit();
                return limit > 0 ? limit : Constants.DEFAULT_DETAIL_LIMIT;
            }
        } catch (Exception e) {
            log.warn("AG-08: Failed to load dataset query limit for dataset={}: {}", datasetId,
                    e.getMessage());
        }
        return getDefaultLimit(queryType);
    }

    private long getDefaultLimit(QueryType queryType) {
        return QueryType.AGGREGATE.equals(queryType) ? Constants.DEFAULT_METRIC_LIMIT
                : Constants.DEFAULT_DETAIL_LIMIT;
    }
}
