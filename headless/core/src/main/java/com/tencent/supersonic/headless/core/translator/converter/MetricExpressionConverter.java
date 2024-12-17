package com.tencent.supersonic.headless.core.translator.converter;

import com.tencent.supersonic.common.jsqlparser.SqlReplaceHelper;
import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.headless.api.pojo.Measure;
import com.tencent.supersonic.headless.api.pojo.enums.MetricDefineType;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.core.pojo.OntologyQuery;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.pojo.SqlQuery;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * This converter replaces metric bizName in the S2SQL with calculation expression (if configured).
 */
@Component("MetricExpressionConverter")
@Slf4j
public class MetricExpressionConverter implements QueryConverter {
    @Override
    public boolean accept(QueryStatement queryStatement) {
        return Objects.nonNull(queryStatement.getSqlQuery())
                && StringUtils.isNotBlank(queryStatement.getSqlQuery().getSql());
    }

    @Override
    public void convert(QueryStatement queryStatement) throws Exception {

        SemanticSchemaResp semanticSchema = queryStatement.getSemanticSchema();
        SqlQuery sqlQuery = queryStatement.getSqlQuery();
        OntologyQuery ontologyQuery = queryStatement.getOntologyQuery();

        Map<String, String> bizName2Expr = getMetricExpressions(semanticSchema, ontologyQuery);
        if (!CollectionUtils.isEmpty(bizName2Expr)) {
            String sql = SqlReplaceHelper.replaceSqlByExpression(sqlQuery.getSql(), bizName2Expr);
            sqlQuery.setSql(sql);
        }
    }

    private Map<String, String> getMetricExpressions(SemanticSchemaResp semanticSchema,
            OntologyQuery ontologyQuery) {

        List<MetricSchemaResp> allMetrics = semanticSchema.getMetrics();
        Set<MetricSchemaResp> queryMetrics = ontologyQuery.getMetrics();
        Set<String> queryFields = ontologyQuery.getFields();
        log.debug("begin to generateDerivedMetric {} [{}]", queryMetrics);

        Set<String> allFields = new HashSet<>();
        Map<String, Measure> allMeasures = new HashMap<>();
        semanticSchema.getModelResps().forEach(modelResp -> {
            allFields.addAll(modelResp.getFieldList());
            if (modelResp.getModelDetail().getMeasures() != null) {
                modelResp.getModelDetail().getMeasures()
                        .forEach(measure -> allMeasures.put(measure.getBizName(), measure));
            }
        });

        Map<String, String> visitedMetrics = new HashMap<>();
        Map<String, String> metric2Expr = new HashMap<>();
        for (MetricSchemaResp queryMetric : queryMetrics) {
            String fieldExpr = buildFieldExpr(allMetrics, allMeasures, queryMetric.getExpr(),
                    queryMetric.getMetricDefineType(), visitedMetrics, queryFields);
            // add all fields referenced in the expression
            queryMetric.getFields().addAll(SqlSelectHelper.getFieldsFromExpr(fieldExpr));
            log.debug("derived metric {}->{}", queryMetric.getBizName(), fieldExpr);
            if (!queryMetric.getBizName().equals(fieldExpr)) {
                metric2Expr.put(queryMetric.getBizName(), fieldExpr);
            }
        }

        return metric2Expr;
    }

    private String buildFieldExpr(final List<MetricSchemaResp> metricResps,
            final Map<String, Measure> allMeasures, final String metricExpr,
            final MetricDefineType metricDefineType, Map<String, String> visitedMetric,
            Set<String> queryFields) {
        Set<String> fields = SqlSelectHelper.getFieldsFromExpr(metricExpr);
        if (!CollectionUtils.isEmpty(fields)) {
            Map<String, String> replace = new HashMap<>();
            for (String field : fields) {
                switch (metricDefineType) {
                    case METRIC:
                        // if defineType=METRIC, field should be the bizName of its parent metric
                        Optional<MetricSchemaResp> metricItem = metricResps.stream()
                                .filter(m -> m.getBizName().equalsIgnoreCase(field)).findFirst();
                        if (metricItem.isPresent()) {
                            if (visitedMetric.keySet().contains(field)) {
                                replace.put(field, visitedMetric.get(field));
                                break;
                            }
                            replace.put(field,
                                    buildFieldExpr(metricResps, allMeasures,
                                            metricItem.get().getExpr(),
                                            metricItem.get().getMetricDefineType(), visitedMetric,
                                            queryFields));
                            visitedMetric.put(field, replace.get(field));
                        }
                        break;
                    case MEASURE:
                        // if defineType=MEASURE, field should be the bizName of its measure
                        if (allMeasures.containsKey(field)) {
                            Measure measure = allMeasures.get(field);
                            String expr = metricExpr;
                            if (Objects.nonNull(measure.getAgg())) {
                                expr = String.format("%s (%s)", measure.getAgg(), metricExpr);
                            }
                            replace.put(field, expr);
                            queryFields.add(field);
                        }
                        break;
                    case FIELD:
                        queryFields.add(field);
                        break;
                    default:
                        break;
                }
            }
            if (!CollectionUtils.isEmpty(replace)) {
                String expr = SqlReplaceHelper.replaceExpression(metricExpr, replace);
                log.debug("derived measure {}->{}", metricExpr, expr);
                return expr;
            }
        }
        return metricExpr;
    }

}
