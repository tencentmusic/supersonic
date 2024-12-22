package com.tencent.supersonic.headless.core.translator.parser;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.supersonic.common.jsqlparser.SqlReplaceHelper;
import com.tencent.supersonic.common.jsqlparser.SqlSelectFunctionHelper;
import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.core.pojo.Ontology;
import com.tencent.supersonic.headless.core.pojo.OntologyQuery;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.pojo.SqlQuery;
import com.tencent.supersonic.headless.core.utils.SqlGenerateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This parser rewrites S2SQL including conversion from metric/dimension name to bizName and build
 * ontology query in preparation for generation of physical SQL.
 */
@Component("SqlQueryParser")
@Slf4j
public class SqlQueryParser implements QueryParser {

    @Override
    public boolean accept(QueryStatement queryStatement) {
        return Objects.nonNull(queryStatement.getSqlQuery()) && queryStatement.getIsS2SQL();
    }

    @Override
    public void parse(QueryStatement queryStatement) throws Exception {
        convertNameToBizName(queryStatement);
        rewriteOrderBy(queryStatement);

        // fill sqlQuery
        SqlQuery sqlQuery = queryStatement.getSqlQuery();
        String tableName = SqlSelectHelper.getTableName(sqlQuery.getSql());
        if (StringUtils.isEmpty(tableName)) {
            return;
        }
        sqlQuery.setTable(tableName.toLowerCase());
        SqlGenerateUtils sqlGenerateUtils = ContextUtils.getBean(SqlGenerateUtils.class);
        SemanticSchemaResp semanticSchema = queryStatement.getSemanticSchema();
        if (!sqlGenerateUtils.isSupportWith(
                EngineType.fromString(semanticSchema.getDatabaseResp().getType().toUpperCase()),
                semanticSchema.getDatabaseResp().getVersion())) {
            sqlQuery.setSupportWith(false);
            sqlQuery.setWithAlias(false);
        }

        // build ontologyQuery
        Ontology ontology = queryStatement.getOntology();
        List<String> allQueryFields = SqlSelectHelper.getAllSelectFields(sqlQuery.getSql());
        OntologyQuery ontologyQuery = new OntologyQuery();
        queryStatement.setOntologyQuery(ontologyQuery);

        List<MetricSchemaResp> queryMetrics = findQueryMetrics(ontology, allQueryFields);
        ontologyQuery.getMetrics().addAll(queryMetrics);

        List<DimSchemaResp> queryDimensions = findQueryDimensions(ontology, allQueryFields);
        ontologyQuery.getDimensions().addAll(queryDimensions);

        List<ModelResp> queryModels = findQueryModels(ontology, queryMetrics, queryDimensions);
        ontologyQuery.getModels().addAll(queryModels);

        AggOption sqlQueryAggOption = getAggOption(sqlQuery.getSql(), queryMetrics);
        ontologyQuery.setAggOption(sqlQueryAggOption);

        log.info("parse sqlQuery [{}] ", sqlQuery);
    }

    private AggOption getAggOption(String sql, List<MetricSchemaResp> metricSchemas) {
        if (SqlSelectFunctionHelper.hasAggregateFunction(sql)) {
            return AggOption.AGGREGATION;
        }

        if (!SqlSelectFunctionHelper.hasAggregateFunction(sql) && !SqlSelectHelper.hasGroupBy(sql)
                && !SqlSelectHelper.hasWith(sql) && !SqlSelectHelper.hasSubSelect(sql)) {
            log.debug("getAggOption simple sql set to DEFAULT");
            return AggOption.NATIVE;
        }

        // if there is no group by in S2SQL,set MetricTable's aggOption to "NATIVE"
        // if there is count() in S2SQL,set MetricTable's aggOption to "NATIVE"
        if (!SqlSelectFunctionHelper.hasAggregateFunction(sql)
                || SqlSelectFunctionHelper.hasFunction(sql, "count")
                || SqlSelectFunctionHelper.hasFunction(sql, "count_distinct")) {
            return AggOption.OUTER;
        }

        if (SqlSelectHelper.hasSubSelect(sql) || SqlSelectHelper.hasWith(sql)
                || SqlSelectHelper.hasGroupBy(sql)) {
            return AggOption.OUTER;
        }
        long defaultAggNullCnt = metricSchemas.stream().filter(
                m -> Objects.isNull(m.getDefaultAgg()) || StringUtils.isBlank(m.getDefaultAgg()))
                .count();
        if (defaultAggNullCnt > 0) {
            log.debug("getAggOption find null defaultAgg metric set to NATIVE");
            return AggOption.DEFAULT;
        }
        return AggOption.DEFAULT;
    }

    private void convertNameToBizName(QueryStatement queryStatement) {
        SemanticSchemaResp semanticSchema = queryStatement.getSemanticSchema();
        Map<String, String> fieldNameToBizNameMap = semanticSchema.getNameToBizNameMap();
        String sql = queryStatement.getSqlQuery().getSql();
        log.debug("dataSetId:{},convert name to bizName before:{}", queryStatement.getDataSetId(),
                sql);
        sql = SqlReplaceHelper.replaceFields(sql, fieldNameToBizNameMap, true);
        log.debug("dataSetId:{},convert name to bizName after:{}", queryStatement.getDataSetId(),
                sql);
        sql = SqlReplaceHelper.replaceTable(sql,
                Constants.TABLE_PREFIX + queryStatement.getDataSetId());
        log.debug("replaceTableName after:{}", sql);
        queryStatement.getSqlQuery().setSql(sql);
    }

    private void rewriteOrderBy(QueryStatement queryStatement) {
        // replace order by field with the select sequence number
        String sql = queryStatement.getSqlQuery().getSql();
        String newSql = SqlReplaceHelper.replaceAggAliasOrderbyField(sql);
        log.debug("replaceOrderAggSameAlias {} -> {}", sql, newSql);
        queryStatement.getSqlQuery().setSql(newSql);
    }

    public List<MetricSchemaResp> findQueryMetrics(Ontology ontology, List<String> bizNames) {
        Map<String, MetricSchemaResp> metricLowerToNameMap = ontology.getMetrics().stream().collect(
                Collectors.toMap(entry -> entry.getBizName().toLowerCase(), entry -> entry));
        return bizNames.stream().map(String::toLowerCase)
                .filter(entry -> metricLowerToNameMap.containsKey(entry))
                .map(entry -> metricLowerToNameMap.get(entry)).collect(Collectors.toList());
    }

    public List<DimSchemaResp> findQueryDimensions(Ontology ontology, List<String> bizNames) {
        Map<String, DimSchemaResp> dimLowerToNameMap = ontology.getDimensions().stream().collect(
                Collectors.toMap(entry -> entry.getBizName().toLowerCase(), entry -> entry));
        return bizNames.stream().map(String::toLowerCase)
                .filter(entry -> dimLowerToNameMap.containsKey(entry))
                .map(entry -> dimLowerToNameMap.get(entry)).collect(Collectors.toList());
    }

    public List<ModelResp> findQueryModels(Ontology ontology, List<MetricSchemaResp> queryMetrics,
            List<DimSchemaResp> queryDimensions) {
        // first, sort models based on the number of query metrics
        Map<String, Integer> modelMetricCount = Maps.newHashMap();
        queryMetrics.forEach(m -> {
            if (!modelMetricCount.containsKey(m.getModelBizName())) {
                modelMetricCount.put(m.getModelBizName(), 1);
            } else {
                int count = modelMetricCount.get(m.getModelBizName());
                modelMetricCount.put(m.getModelBizName(), count + 1);
            }
        });
        List<String> metricsDataModels = modelMetricCount.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).map(e -> e.getKey())
                .collect(Collectors.toList());

        // second, sort models based on the number of query dimensions
        Map<String, Integer> modelDimCount = Maps.newHashMap();
        queryDimensions.forEach(m -> {
            if (!modelDimCount.containsKey(m.getModelBizName())) {
                modelDimCount.put(m.getModelBizName(), 1);
            } else {
                int count = modelDimCount.get(m.getModelBizName());
                modelDimCount.put(m.getModelBizName(), count + 1);
            }
        });
        List<String> dimDataModels = modelDimCount.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).map(e -> e.getKey())
                .collect(Collectors.toList());

        Set<String> dataModelNames = Sets.newLinkedHashSet();
        dataModelNames.addAll(dimDataModels);
        dataModelNames.addAll(metricsDataModels);
        return dataModelNames.stream().map(bizName -> ontology.getModelMap().get(bizName))
                .collect(Collectors.toList());
    }

}
