package com.tencent.supersonic.headless.core.translator.converter;

import com.tencent.supersonic.common.jsqlparser.SqlReplaceHelper;
import com.tencent.supersonic.common.jsqlparser.SqlSelectFunctionHelper;
import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.pojo.SqlQueryParam;
import com.tencent.supersonic.headless.core.translator.parser.s2sql.OntologyQueryParam;
import com.tencent.supersonic.headless.core.utils.SqlGenerateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component("SqlQueryConverter")
@Slf4j
public class SqlQueryConverter implements QueryConverter {

    @Override
    public boolean accept(QueryStatement queryStatement) {
        return Objects.nonNull(queryStatement.getSqlQueryParam()) && queryStatement.getIsS2SQL();
    }

    @Override
    public void convert(QueryStatement queryStatement) throws Exception {
        convertNameToBizName(queryStatement);
        rewriteOrderBy(queryStatement);

        // fill sqlQuery
        SemanticSchemaResp semanticSchemaResp = queryStatement.getSemanticSchemaResp();
        SqlQueryParam sqlQueryParam = queryStatement.getSqlQueryParam();
        String tableName = SqlSelectHelper.getTableName(sqlQueryParam.getSql());
        if (StringUtils.isEmpty(tableName)) {
            return;
        }
        sqlQueryParam.setTable(tableName.toLowerCase());
        SqlGenerateUtils sqlGenerateUtils = ContextUtils.getBean(SqlGenerateUtils.class);
        if (!sqlGenerateUtils.isSupportWith(
                EngineType.fromString(semanticSchemaResp.getDatabaseResp().getType().toUpperCase()),
                semanticSchemaResp.getDatabaseResp().getVersion())) {
            sqlQueryParam.setSupportWith(false);
            sqlQueryParam.setWithAlias(false);
        }

        // build ontologyQuery
        List<String> allFields = SqlSelectHelper.getAllSelectFields(sqlQueryParam.getSql());
        List<MetricSchemaResp> metricSchemas = getMetrics(semanticSchemaResp, allFields);
        List<String> metrics =
                metricSchemas.stream().map(SchemaItem::getBizName).collect(Collectors.toList());
        Set<String> dimensions = getDimensions(semanticSchemaResp, allFields);
        OntologyQueryParam ontologyQueryParam = new OntologyQueryParam();
        ontologyQueryParam.getMetrics().addAll(metrics);
        ontologyQueryParam.getDimensions().addAll(dimensions);
        AggOption sqlQueryAggOption = getAggOption(sqlQueryParam.getSql(), metricSchemas);

        // if sql query itself has aggregation, ontology query just returns detail
        if (sqlQueryAggOption.equals(AggOption.AGGREGATION)) {
            ontologyQueryParam.setAggOption(AggOption.NATIVE);
        } else if (sqlQueryAggOption.equals(AggOption.NATIVE) && !metrics.isEmpty()) {
            ontologyQueryParam.setAggOption(AggOption.DEFAULT);
        }
        ontologyQueryParam.setNativeQuery(!AggOption.isAgg(ontologyQueryParam.getAggOption()));
        queryStatement.setOntologyQueryParam(ontologyQueryParam);
        queryStatement.setSql(sqlQueryParam.getSql());
        log.info("parse sqlQuery [{}] ", sqlQueryParam);
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

    private Set<String> getDimensions(SemanticSchemaResp semanticSchemaResp,
            List<String> allFields) {
        Map<String, String> dimensionLowerToNameMap = semanticSchemaResp.getDimensions().stream()
                .collect(Collectors.toMap(entry -> entry.getBizName().toLowerCase(),
                        SchemaItem::getBizName, (k1, k2) -> k1));
        // dimensionLowerToNameMap.put(TimeDimensionEnum.DAY.getName(),
        // TimeDimensionEnum.DAY.getName());
        return allFields.stream()
                .filter(entry -> dimensionLowerToNameMap.containsKey(entry.toLowerCase()))
                .map(entry -> dimensionLowerToNameMap.get(entry.toLowerCase()))
                .collect(Collectors.toSet());
    }

    private List<MetricSchemaResp> getMetrics(SemanticSchemaResp semanticSchemaResp,
            List<String> allFields) {
        Map<String, MetricSchemaResp> metricLowerToNameMap =
                semanticSchemaResp.getMetrics().stream().collect(Collectors
                        .toMap(entry -> entry.getBizName().toLowerCase(), entry -> entry));
        return allFields.stream()
                .filter(entry -> metricLowerToNameMap.containsKey(entry.toLowerCase()))
                .map(entry -> metricLowerToNameMap.get(entry.toLowerCase()))
                .collect(Collectors.toList());
    }

    private void convertNameToBizName(QueryStatement queryStatement) {
        SemanticSchemaResp semanticSchemaResp = queryStatement.getSemanticSchemaResp();
        Map<String, String> fieldNameToBizNameMap = getFieldNameToBizNameMap(semanticSchemaResp);
        String sql = queryStatement.getSqlQueryParam().getSql();
        log.debug("dataSetId:{},convert name to bizName before:{}", queryStatement.getDataSetId(),
                sql);
        sql = SqlReplaceHelper.replaceFields(sql, fieldNameToBizNameMap, true);
        log.debug("dataSetId:{},convert name to bizName after:{}", queryStatement.getDataSetId(),
                sql);
        sql = SqlReplaceHelper.replaceTable(sql,
                Constants.TABLE_PREFIX + queryStatement.getDataSetId());
        log.debug("replaceTableName after:{}", sql);
        queryStatement.getSqlQueryParam().setSql(sql);
    }

    private void rewriteOrderBy(QueryStatement queryStatement) {
        // replace order by field with the select sequence number
        String sql = queryStatement.getSqlQueryParam().getSql();
        String newSql = SqlReplaceHelper.replaceAggAliasOrderbyField(sql);
        log.debug("replaceOrderAggSameAlias {} -> {}", sql, newSql);
        queryStatement.getSqlQueryParam().setSql(newSql);
    }

    protected Map<String, String> getFieldNameToBizNameMap(SemanticSchemaResp semanticSchemaResp) {
        // support fieldName and field alias to bizName
        Map<String, String> dimensionResults = semanticSchemaResp.getDimensions().stream().flatMap(
                entry -> getPairStream(entry.getAlias(), entry.getName(), entry.getBizName()))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight, (k1, k2) -> k1));

        Map<String, String> metricResults = semanticSchemaResp.getMetrics().stream().flatMap(
                entry -> getPairStream(entry.getAlias(), entry.getName(), entry.getBizName()))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight, (k1, k2) -> k1));

        dimensionResults.putAll(metricResults);
        return dimensionResults;
    }

    private Stream<Pair<String, String>> getPairStream(String aliasStr, String name,
            String bizName) {
        Set<Pair<String, String>> elements = new HashSet<>();
        elements.add(Pair.of(name, bizName));
        if (StringUtils.isNotBlank(aliasStr)) {
            List<String> aliasList = SchemaItem.getAliasList(aliasStr);
            for (String alias : aliasList) {
                elements.add(Pair.of(alias, bizName));
            }
        }
        return elements.stream();
    }

}
