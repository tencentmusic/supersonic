package com.tencent.supersonic.headless.core.translator.parser;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.common.jsqlparser.SqlReplaceHelper;
import com.tencent.supersonic.common.jsqlparser.SqlSelectFunctionHelper;
import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.QueryState;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.core.pojo.Ontology;
import com.tencent.supersonic.headless.core.pojo.OntologyQuery;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.pojo.SqlQuery;
import com.tencent.supersonic.headless.core.utils.SqlGenerateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        // build ontologyQuery
        SqlQuery sqlQuery = queryStatement.getSqlQuery();
        List<String> queryFields = SqlSelectHelper.getAllSelectFields(sqlQuery.getSql());
        Set<String> queryAliases = SqlSelectHelper.getAliasFields(sqlQuery.getSql());
        queryFields.removeAll(queryAliases);
        Ontology ontology = queryStatement.getOntology();
        OntologyQuery ontologyQuery = buildOntologyQuery(ontology, queryFields);
        // check if there are fields not matched with any metric or dimension
        if (queryFields.size() > ontologyQuery.getMetrics().size()
                + ontologyQuery.getDimensions().size()) {
            List<String> semanticFields = Lists.newArrayList();
            ontologyQuery.getMetrics().forEach(m -> semanticFields.add(m.getName()));
            ontologyQuery.getDimensions().forEach(d -> semanticFields.add(d.getName()));
            String errMsg =
                    String.format("Querying columns[%s] not matched with semantic fields[%s].",
                            queryFields, semanticFields);
            queryStatement.setErrMsg(errMsg);
            queryStatement.setStatus(QueryState.INVALID);
            return;
        }
        queryStatement.setOntologyQuery(ontologyQuery);

        AggOption sqlQueryAggOption = getAggOption(sqlQuery.getSql(), ontologyQuery.getMetrics());
        ontologyQuery.setAggOption(sqlQueryAggOption);

        convertNameToBizName(queryStatement);
        // Solve the problem of SQL execution error when alias is Chinese
        aliasesWithBackticks(queryStatement);
        rewriteOrderBy(queryStatement);

        // fill sqlQuery
        String tableName = SqlSelectHelper.getTableName(sqlQuery.getSql());
        if (StringUtils.isEmpty(tableName)) {
            return;
        }
        sqlQuery.setTable(Constants.TABLE_PREFIX + queryStatement.getDataSetId());
        SqlGenerateUtils sqlGenerateUtils = ContextUtils.getBean(SqlGenerateUtils.class);
        SemanticSchemaResp semanticSchema = queryStatement.getSemanticSchema();
        if (!sqlGenerateUtils.isSupportWith(
                EngineType.fromString(semanticSchema.getDatabaseResp().getType().toUpperCase()),
                semanticSchema.getDatabaseResp().getVersion())) {
            sqlQuery.setSupportWith(false);
            sqlQuery.setWithAlias(false);
        }

        log.info("parse sqlQuery [{}] ", sqlQuery);
    }

    private void aliasesWithBackticks(QueryStatement queryStatement) {
        String sql = queryStatement.getSqlQuery().getSql();
        sql = SqlReplaceHelper.replaceAliasWithBackticks(sql);
        queryStatement.getSqlQuery().setSql(sql);
    }

    private AggOption getAggOption(String sql, Set<MetricSchemaResp> metricSchemas) {
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

    private Map<String, String> getNameToBizNameMap(OntologyQuery query) {
        // support fieldName and field alias to bizName
        Map<String, String> dimensionResults = query.getDimensions().stream().flatMap(
                entry -> getPairStream(entry.getAlias(), entry.getName(), entry.getBizName()))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight, (k1, k2) -> k1));

        Map<String, String> metricResults = query.getMetrics().stream().flatMap(
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

    private void convertNameToBizName(QueryStatement queryStatement) {
        Map<String, String> fieldNameToBizNameMap =
                getNameToBizNameMap(queryStatement.getOntologyQuery());
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

    private OntologyQuery buildOntologyQuery(Ontology ontology, List<String> queryFields) {
        OntologyQuery ontologyQuery = new OntologyQuery();
        Set<String> fields = Sets.newHashSet(queryFields);

        // find belonging model for every querying metrics
        ontology.getMetricMap().entrySet().forEach(entry -> {
            String modelName = entry.getKey();
            entry.getValue().forEach(m -> {
                if (fields.contains(m.getName()) || fields.contains(m.getBizName())) {
                    ontologyQuery.getModelMap().put(modelName,
                            ontology.getModelMap().get(modelName));
                    ontologyQuery.getMetricMap().computeIfAbsent(modelName, k -> Sets.newHashSet())
                            .add(m);
                    fields.remove(m.getName());
                    fields.remove(m.getBizName());
                }
            });
        });

        // first try to find all querying dimensions in the models with querying metrics.
        ontology.getDimensionMap().entrySet().stream()
                .filter(entry -> ontologyQuery.getMetricMap().containsKey(entry.getKey()))
                .forEach(entry -> {
                    String modelName = entry.getKey();
                    entry.getValue().forEach(d -> {
                        if (fields.contains(d.getName()) || fields.contains(d.getBizName())) {
                            ontologyQuery.getModelMap().put(modelName,
                                    ontology.getModelMap().get(modelName));
                            ontologyQuery.getDimensionMap()
                                    .computeIfAbsent(modelName, k -> Sets.newHashSet()).add(d);
                            fields.remove(d.getName());
                            fields.remove(d.getBizName());
                        }
                    });
                });

        // second, try to find a model that has all the remaining fields, such that no further join
        // is needed.
        if (!fields.isEmpty()) {
            Map<String, Set<DimSchemaResp>> model2dims = new HashMap<>();
            ontology.getDimensionMap().entrySet().forEach(entry -> {
                String modelName = entry.getKey();
                entry.getValue().forEach(d -> {
                    if (fields.contains(d.getName()) || fields.contains(d.getBizName())) {
                        model2dims.computeIfAbsent(modelName, k -> Sets.newHashSet()).add(d);
                    }
                });
            });
            Optional<Map.Entry<String, Set<DimSchemaResp>>> modelEntry = model2dims.entrySet()
                    .stream().filter(entry -> entry.getValue().size() == fields.size()).findFirst();
            if (modelEntry.isPresent()) {
                ontologyQuery.getDimensionMap().put(modelEntry.get().getKey(),
                        modelEntry.get().getValue());
                ontologyQuery.getModelMap().put(modelEntry.get().getKey(),
                        ontology.getModelMap().get(modelEntry.get().getKey()));
                fields.clear();
            }
        }

        // finally if there are still fields not found belonging models, try to find in the models
        // iteratively
        if (!fields.isEmpty()) {
            ontology.getDimensionMap().entrySet().forEach(entry -> {
                String modelName = entry.getKey();
                if (!ontologyQuery.getDimensionMap().containsKey(modelName)) {
                    entry.getValue().forEach(d -> {
                        if (fields.contains(d.getName()) || fields.contains(d.getBizName())) {
                            ontologyQuery.getModelMap().put(modelName,
                                    ontology.getModelMap().get(modelName));
                            ontologyQuery.getDimensionMap()
                                    .computeIfAbsent(modelName, k -> Sets.newHashSet()).add(d);
                            fields.remove(d.getName());
                            fields.remove(d.getBizName());
                        }
                    });
                }
            });
        }

        return ontologyQuery;
    }

}
