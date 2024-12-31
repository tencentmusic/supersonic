package com.tencent.supersonic.headless.core.translator.parser;

import com.tencent.supersonic.common.jsqlparser.SqlAsHelper;
import com.tencent.supersonic.common.jsqlparser.SqlReplaceHelper;
import com.tencent.supersonic.common.jsqlparser.SqlSelectFunctionHelper;
import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.Measure;
import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import com.tencent.supersonic.headless.api.pojo.enums.MetricType;
import com.tencent.supersonic.headless.api.pojo.response.*;
import com.tencent.supersonic.headless.core.pojo.OntologyQuery;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.pojo.SqlQuery;
import com.tencent.supersonic.headless.core.utils.SqlGenerateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        SemanticSchemaResp semanticSchemaResp = queryStatement.getSemanticSchema();
        SqlQuery sqlQuery = queryStatement.getSqlQuery();
        String tableName = SqlSelectHelper.getTableName(sqlQuery.getSql());
        if (StringUtils.isEmpty(tableName)) {
            return;
        }
        sqlQuery.setTable(tableName.toLowerCase());
        SqlGenerateUtils sqlGenerateUtils = ContextUtils.getBean(SqlGenerateUtils.class);
        if (!sqlGenerateUtils.isSupportWith(
                EngineType.fromString(semanticSchemaResp.getDatabaseResp().getType().toUpperCase()),
                semanticSchemaResp.getDatabaseResp().getVersion())) {
            sqlQuery.setSupportWith(false);
            sqlQuery.setWithAlias(false);
        }

        // build ontologyQuery
        List<String> allFields = SqlSelectHelper.getAllSelectFields(sqlQuery.getSql());
        List<MetricSchemaResp> metricSchemas = getMetrics(semanticSchemaResp, allFields);
        List<String> metrics =
                metricSchemas.stream().map(SchemaItem::getBizName).collect(Collectors.toList());
        Set<String> dimensions = getDimensions(semanticSchemaResp, allFields);
        // check if there are fields not matched with any metric or dimension
        if (allFields.size() > metricSchemas.size() + dimensions.size()) {
            queryStatement
                    .setErrMsg("There are querying columns in the SQL not matched with any semantic field.");
            queryStatement.setStatus(1);
            return;
        }

        OntologyQuery ontologyQuery = new OntologyQuery();
        ontologyQuery.getMetrics().addAll(metrics);
        ontologyQuery.getDimensions().addAll(dimensions);
        AggOption sqlQueryAggOption = getAggOption(sqlQuery.getSql(), metricSchemas);
        // if sql query itself has aggregation, ontology query just returns detail
        if (sqlQueryAggOption.equals(AggOption.AGGREGATION)) {
            ontologyQuery.setAggOption(AggOption.NATIVE);
        } else if (sqlQueryAggOption.equals(AggOption.NATIVE) && !metrics.isEmpty()) {
            ontologyQuery.setAggOption(AggOption.DEFAULT);
        }
        ontologyQuery.setNativeQuery(!AggOption.isAgg(ontologyQuery.getAggOption()));
        queryStatement.setOntologyQuery(ontologyQuery);

        generateDerivedMetric(sqlGenerateUtils, queryStatement);

        queryStatement.setSql(sqlQuery.getSql());
        // replace sql fields for db, must called after convertNameToBizName
        String sqlRewrite = replaceSqlFieldsForHanaDB(queryStatement, sqlQuery.getSql());
        sqlQuery.setSql(sqlRewrite);
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

    private Set<String> getDimensions(SemanticSchemaResp semanticSchemaResp,
            List<String> allFields) {
        Map<String, String> dimensionLowerToNameMap = semanticSchemaResp.getDimensions().stream()
                .collect(Collectors.toMap(entry -> entry.getBizName().toLowerCase(),
                        SchemaItem::getBizName, (k1, k2) -> k1));
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


    private void generateDerivedMetric(SqlGenerateUtils sqlGenerateUtils,
            QueryStatement queryStatement) {
        SemanticSchemaResp semanticSchemaResp = queryStatement.getSemanticSchema();
        SqlQuery sqlParam = queryStatement.getSqlQuery();
        OntologyQuery ontologyParam = queryStatement.getOntologyQuery();
        String sql = sqlParam.getSql();

        Set<String> measures = new HashSet<>();
        Map<String, String> replaces = generateDerivedMetric(sqlGenerateUtils, semanticSchemaResp,
                ontologyParam.getAggOption(), ontologyParam.getMetrics(),
                ontologyParam.getDimensions(), measures);

        if (!CollectionUtils.isEmpty(replaces)) {
            // metricTable sql use measures replace metric
            sql = SqlReplaceHelper.replaceSqlByExpression(sql, replaces);
            ontologyParam.setAggOption(AggOption.NATIVE);
            // metricTable use measures replace metric
            if (!CollectionUtils.isEmpty(measures)) {
                ontologyParam.getMetrics().addAll(measures);
            } else {
                // empty measure , fill default
                ontologyParam.getMetrics().add(sqlGenerateUtils.generateInternalMetricName(
                        getDefaultModel(semanticSchemaResp, ontologyParam.getDimensions())));
            }
        }

        sqlParam.setSql(sql);
    }

    private Map<String, String> generateDerivedMetric(SqlGenerateUtils sqlGenerateUtils,
            SemanticSchemaResp semanticSchemaResp, AggOption aggOption, Set<String> metrics,
            Set<String> dimensions, Set<String> measures) {
        Map<String, String> result = new HashMap<>();
        List<MetricSchemaResp> metricResps = semanticSchemaResp.getMetrics();
        List<DimSchemaResp> dimensionResps = semanticSchemaResp.getDimensions();

        // Check if any metric is derived
        boolean hasDerivedMetrics =
                metricResps.stream().anyMatch(m -> metrics.contains(m.getBizName()) && MetricType
                        .isDerived(m.getMetricDefineType(), m.getMetricDefineByMeasureParams()));
        if (!hasDerivedMetrics) {
            return result;
        }

        log.debug("begin to generateDerivedMetric {} [{}]", aggOption, metrics);

        Set<String> allFields = new HashSet<>();
        Map<String, Measure> allMeasures = new HashMap<>();
        semanticSchemaResp.getModelResps().forEach(modelResp -> {
            allFields.addAll(modelResp.getFieldList());
            if (modelResp.getModelDetail().getMeasures() != null) {
                modelResp.getModelDetail().getMeasures()
                        .forEach(measure -> allMeasures.put(measure.getBizName(), measure));
            }
        });

        Set<String> derivedDimensions = new HashSet<>();
        Set<String> derivedMetrics = new HashSet<>();
        Map<String, String> visitedMetrics = new HashMap<>();

        for (MetricResp metricResp : metricResps) {
            if (metrics.contains(metricResp.getBizName())) {
                boolean isDerived = MetricType.isDerived(metricResp.getMetricDefineType(),
                        metricResp.getMetricDefineByMeasureParams());
                if (isDerived) {
                    String expr = sqlGenerateUtils.generateDerivedMetric(metricResps, allFields,
                            allMeasures, dimensionResps, sqlGenerateUtils.getExpr(metricResp),
                            metricResp.getMetricDefineType(), aggOption, visitedMetrics,
                            derivedMetrics, derivedDimensions);
                    result.put(metricResp.getBizName(), expr);
                    log.debug("derived metric {}->{}", metricResp.getBizName(), expr);
                } else {
                    measures.add(metricResp.getBizName());
                }
            }
        }

        measures.addAll(derivedMetrics);
        derivedDimensions.stream().filter(dimension -> !dimensions.contains(dimension))
                .forEach(dimensions::add);

        return result;
    }


    /**
     * special process for hanaDB,the sap hana DB don't support the chinese name as the column name,
     * so we need to quote the column name after converting the convertNameToBizName called
     * 
     * sap hana DB will auto translate the colume to upper case letter if not quoted. also we need
     * to quote the field name if it is a lower case letter.
     * 
     * @param queryStatement
     * @param sql
     * @return
     */
    private String replaceSqlFieldsForHanaDB(QueryStatement queryStatement, String sql) {
        SemanticSchemaResp semanticSchemaResp = queryStatement.getSemanticSchema();
        if (!semanticSchemaResp.getDatabaseResp().getType()
                .equalsIgnoreCase(EngineType.HANADB.getName())) {
            return sql;
        }
        Map<String, String> fieldNameToBizNameMap = getFieldNameToBizNameMap(semanticSchemaResp);

        Map<String, String> fieldNameToBizNameMapQuote = new HashMap<>();
        fieldNameToBizNameMap.forEach((key, value) -> {
            if (!fieldNameToBizNameMapQuote.containsKey(value) && !value.matches("\".*\"")
                    && !value.matches("[A-Z0-9_].*?")) {
                fieldNameToBizNameMapQuote.put(value, "\"" + value + "\"");
            }
        });
        String sqlNew = sql;
        if (fieldNameToBizNameMapQuote.size() > 0) {
            sqlNew = SqlReplaceHelper.replaceFields(sql, fieldNameToBizNameMapQuote, true);
        }
        // replace alias field name
        List<String> asFields = SqlAsHelper.getAsFields(sqlNew);
        Map<String, String> fieldMapput = new HashMap<>();
        for (String asField : asFields) {
            String value = asField;
            if (!value.matches("\".*?\"") && !value.matches("[A-Z0-9_].*?")) {
                value = "\"" + asField + "\"";
                fieldMapput.put(asField, value);
            }
        }
        if (fieldMapput.size() > 0) {
            sqlNew = SqlReplaceHelper.replaceAliasFieldName(sqlNew, fieldMapput);
        }
        return sqlNew;
    }

    private void convertNameToBizName(QueryStatement queryStatement) {
        SemanticSchemaResp semanticSchemaResp = queryStatement.getSemanticSchema();
        Map<String, String> fieldNameToBizNameMap = getFieldNameToBizNameMap(semanticSchemaResp);
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

    private String getDefaultModel(SemanticSchemaResp semanticSchemaResp, Set<String> dimensions) {
        if (!CollectionUtils.isEmpty(dimensions)) {
            Map<String, Long> modelMatchCnt = new HashMap<>();
            for (ModelResp modelResp : semanticSchemaResp.getModelResps()) {
                modelMatchCnt.put(modelResp.getBizName(), modelResp.getModelDetail().getDimensions()
                        .stream().filter(d -> dimensions.contains(d.getBizName())).count());
            }
            return modelMatchCnt.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .map(Map.Entry::getKey).findFirst().orElse("");
        }
        return semanticSchemaResp.getModelResps().get(0).getBizName();
    }

}
