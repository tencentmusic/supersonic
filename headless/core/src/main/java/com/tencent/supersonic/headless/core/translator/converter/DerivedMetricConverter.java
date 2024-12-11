package com.tencent.supersonic.headless.core.translator.converter;

import com.tencent.supersonic.common.jsqlparser.SqlReplaceHelper;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.Measure;
import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import com.tencent.supersonic.headless.api.pojo.enums.MetricType;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.pojo.SqlQueryParam;
import com.tencent.supersonic.headless.core.translator.parser.s2sql.OntologyQueryParam;
import com.tencent.supersonic.headless.core.utils.SqlGenerateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Component("DerivedMetricConverter")
@Slf4j
public class DerivedMetricConverter implements QueryConverter {
    @Override
    public boolean accept(QueryStatement queryStatement) {
        return Objects.nonNull(queryStatement.getSqlQueryParam())
                && StringUtils.isNotBlank(queryStatement.getSqlQueryParam().getSql());
    }

    @Override
    public void convert(QueryStatement queryStatement) throws Exception {

        SemanticSchemaResp semanticSchemaResp = queryStatement.getSemanticSchemaResp();
        SqlQueryParam sqlParam = queryStatement.getSqlQueryParam();
        OntologyQueryParam ontologyParam = queryStatement.getOntologyQueryParam();
        String sql = sqlParam.getSql();

        Set<String> measures = new HashSet<>();
        Map<String, String> replaces =
                generateDerivedMetric(semanticSchemaResp, ontologyParam.getAggOption(),
                        ontologyParam.getMetrics(), ontologyParam.getDimensions(), measures);

        if (!CollectionUtils.isEmpty(replaces)) {
            // metricTable sql use measures replace metric
            sql = SqlReplaceHelper.replaceSqlByExpression(sql, replaces);
            ontologyParam.setAggOption(AggOption.NATIVE);
            // metricTable use measures replace metric
            if (!CollectionUtils.isEmpty(measures)) {
                ontologyParam.getMetrics().addAll(measures);
            }
        }

        sqlParam.setSql(sql);
        queryStatement.setSql(queryStatement.getSqlQueryParam().getSql());
    }

    private Map<String, String> generateDerivedMetric(SemanticSchemaResp semanticSchemaResp,
            AggOption aggOption, Set<String> metrics, Set<String> dimensions,
            Set<String> measures) {
        SqlGenerateUtils sqlGenerateUtils = ContextUtils.getBean(SqlGenerateUtils.class);
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

}
