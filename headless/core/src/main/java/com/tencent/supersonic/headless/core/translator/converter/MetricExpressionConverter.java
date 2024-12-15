package com.tencent.supersonic.headless.core.translator.converter;

import com.tencent.supersonic.common.jsqlparser.SqlReplaceHelper;
import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.headless.api.pojo.Measure;
import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import com.tencent.supersonic.headless.api.pojo.enums.MetricDefineType;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
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
 * This converter replaces metric fields in the S2SQL with calculation expressions (if configured).
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

        Map<String, String> metric2Expr = getMetricExpressions(semanticSchema, ontologyQuery);
        if (!CollectionUtils.isEmpty(metric2Expr)) {
            String sql = SqlReplaceHelper.replaceSqlByExpression(sqlQuery.getSql(), metric2Expr);
            sqlQuery.setSql(sql);
            ontologyQuery.setAggOption(AggOption.NATIVE);
        }
    }

    private Map<String, String> getMetricExpressions(SemanticSchemaResp semanticSchema,
            OntologyQuery ontologyQuery) {

        List<MetricSchemaResp> allMetrics = semanticSchema.getMetrics();
        List<DimSchemaResp> allDimensions = semanticSchema.getDimensions();
        AggOption aggOption = ontologyQuery.getAggOption();
        Set<MetricSchemaResp> queryMetrics = ontologyQuery.getMetrics();
        Set<DimSchemaResp> queryDimensions = ontologyQuery.getDimensions();
        Set<String> queryFields = ontologyQuery.getFields();
        log.debug("begin to generateDerivedMetric {} [{}]", aggOption, queryMetrics);

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
            String fieldExpr = buildFieldExpr(allMetrics, allFields, allMeasures, allDimensions,
                    queryMetric.getExpr(), queryMetric.getMetricDefineType(), aggOption,
                    visitedMetrics, queryDimensions, queryFields);
            // add all fields referenced in the expression
            queryMetric.getFields().addAll(SqlSelectHelper.getFieldsFromExpr(fieldExpr));
            log.debug("derived metric {}->{}", queryMetric.getBizName(), fieldExpr);
            if (queryMetric.isDerived()) {
                metric2Expr.put(queryMetric.getBizName(), fieldExpr);
            }
        }

        return metric2Expr;
    }

    private String buildFieldExpr(final List<MetricSchemaResp> metricResps,
            final Set<String> allFields, final Map<String, Measure> allMeasures,
            final List<DimSchemaResp> dimensionResps, final String expression,
            final MetricDefineType metricDefineType, AggOption aggOption,
            Map<String, String> visitedMetric, Set<DimSchemaResp> queryDimensions,
            Set<String> queryFields) {
        Set<String> fields = SqlSelectHelper.getFieldsFromExpr(expression);
        if (!CollectionUtils.isEmpty(fields)) {
            Map<String, String> replace = new HashMap<>();
            for (String field : fields) {
                queryFields.add(field);
                switch (metricDefineType) {
                    case METRIC:
                        Optional<MetricSchemaResp> metricItem = metricResps.stream()
                                .filter(m -> m.getBizName().equalsIgnoreCase(field)).findFirst();
                        if (metricItem.isPresent()) {
                            if (visitedMetric.keySet().contains(field)) {
                                replace.put(field, visitedMetric.get(field));
                                break;
                            }
                            replace.put(field,
                                    buildFieldExpr(metricResps, allFields, allMeasures,
                                            dimensionResps, metricItem.get().getExpr(),
                                            metricItem.get().getMetricDefineType(), aggOption,
                                            visitedMetric, queryDimensions, queryFields));
                            visitedMetric.put(field, replace.get(field));
                        }
                        break;
                    case MEASURE:
                        if (allMeasures.containsKey(field)) {
                            Measure measure = allMeasures.get(field);
                            if (AggOperatorEnum.COUNT_DISTINCT.getOperator()
                                    .equalsIgnoreCase(measure.getAgg())) {
                                return AggOption.NATIVE.equals(aggOption) ? measure.getExpr()
                                        : AggOperatorEnum.COUNT.getOperator() + " ( "
                                                + AggOperatorEnum.DISTINCT + " " + measure.getExpr()
                                                + " ) ";
                            }
                            String expr = AggOption.NATIVE.equals(aggOption) ? measure.getExpr()
                                    : measure.getAgg() + " ( " + measure.getExpr() + " ) ";

                            replace.put(field, expr);
                        }
                        break;
                    case FIELD:
                        if (allFields.contains(field)) {
                            Optional<DimSchemaResp> dimensionItem = dimensionResps.stream()
                                    .filter(d -> d.getBizName().equals(field)).findFirst();
                            if (dimensionItem.isPresent()) {
                                queryDimensions.add(dimensionItem.get());
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
            if (!CollectionUtils.isEmpty(replace)) {
                String expr = SqlReplaceHelper.replaceExpression(expression, replace);
                log.debug("derived measure {}->{}", expression, expr);
                return expr;
            }
        }
        return expression;
    }

}
