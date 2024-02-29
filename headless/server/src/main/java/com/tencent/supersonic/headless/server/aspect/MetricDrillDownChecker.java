package com.tencent.supersonic.headless.server.aspect;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import com.tencent.supersonic.common.util.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.headless.api.pojo.DrillDownDimension;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticSchemaResp;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.server.service.MetricService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Aspect
@Component
@Slf4j
public class MetricDrillDownChecker {

    @Autowired
    private MetricService metricService;

    @Around("execution(* com.tencent.supersonic.headless.core.parser.QueryParser.parse(..))")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] objects = joinPoint.getArgs();
        QueryStatement queryStatement = (QueryStatement) objects[0];
        if (queryStatement.getDataSetQueryParam() == null) {
            return joinPoint.proceed();
        }
        checkQuery(queryStatement.getSemanticSchemaResp(), queryStatement.getDataSetQueryParam().getSql());
        return joinPoint.proceed();
    }

    public void checkQuery(SemanticSchemaResp semanticSchemaResp, String sql) {
        List<String> groupByFields = SqlSelectHelper.getGroupByFields(sql);
        List<String> metricFields = SqlSelectHelper.getAggregateAsFields(sql);
        List<String> whereFields = SqlSelectHelper.getWhereFields(sql);
        List<String> dimensionFields = getDimensionFields(groupByFields, whereFields);
        if (CollectionUtils.isEmpty(metricFields) || StringUtils.isBlank(sql)) {
            return;
        }
        for (String metricName : metricFields) {
            MetricSchemaResp metric = semanticSchemaResp.getMetric(metricName);
            List<DimensionResp> necessaryDimensions = getNecessaryDimensions(metric, semanticSchemaResp);
            List<DimensionResp> dimensionsMissing = getNecessaryDimensionMissing(necessaryDimensions, dimensionFields);
            if (!CollectionUtils.isEmpty(dimensionsMissing)) {
                String errMsg = String.format("指标:%s 缺失必要下钻维度:%s", metric.getName(),
                        dimensionsMissing.stream().map(DimensionResp::getName).collect(Collectors.toList()));
                throw new InvalidArgumentException(errMsg);
            }
        }
        for (String dimensionBizName : groupByFields) {
            if (TimeDimensionEnum.containsTimeDimension(dimensionBizName)) {
                continue;
            }
            List<MetricResp> metricResps = getMetrics(metricFields, semanticSchemaResp);
            if (!checkDrillDownDimension(dimensionBizName, metricResps, semanticSchemaResp)) {
                DimSchemaResp dimSchemaResp = semanticSchemaResp.getDimension(dimensionBizName);
                String errMsg = String.format("维度:%s, 不在当前查询指标的下钻维度配置中, 请检查", dimSchemaResp.getName());
                throw new InvalidArgumentException(errMsg);
            }
        }
    }

    /**
     * To check whether the dimension bound to the metric exists,
     * eg: metric like UV is calculated in a certain dimension, it cannot be used on other dimensions.
     */
    private List<DimensionResp> getNecessaryDimensionMissing(List<DimensionResp> necessaryDimensions,
                                                             List<String> dimensionFields) {
        return necessaryDimensions.stream()
                .filter(dimension -> !dimensionFields.contains(dimension.getBizName()))
                .collect(Collectors.toList());
    }

    /**
     * To check whether the dimension can drill down the metric,
     * eg: some descriptive dimensions are not suitable as drill-down dimensions
     */
    private boolean checkDrillDownDimension(String dimensionName,
                                            List<MetricResp> metricResps,
                                            SemanticSchemaResp semanticSchemaResp) {
        if (CollectionUtils.isEmpty(metricResps)) {
            return true;
        }
        List<String> relateDimensions = metricResps.stream()
                .map(this::getDrillDownDimensions)
                .filter(drillDownDimensions -> !CollectionUtils.isEmpty(drillDownDimensions))
                .map(drillDownDimensions -> drillDownDimensions.stream()
                        .map(DrillDownDimension::getDimensionId).collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .map(id -> convertDimensionIdToBizName(id, semanticSchemaResp))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        //if no metric has drill down dimension, return true
        if (CollectionUtils.isEmpty(relateDimensions)) {
            return true;
        }
        //if this dimension not in relate drill-down dimensions, return false
        return relateDimensions.contains(dimensionName);
    }

    private List<DimensionResp> getNecessaryDimensions(MetricSchemaResp metric, SemanticSchemaResp semanticSchemaResp) {
        if (metric == null) {
            return Lists.newArrayList();
        }
        List<DrillDownDimension> drillDownDimensions = getDrillDownDimensions(metric);
        if (CollectionUtils.isEmpty(drillDownDimensions)) {
            return Lists.newArrayList();
        }
        return drillDownDimensions.stream()
                .filter(DrillDownDimension::isNecessary).map(DrillDownDimension::getDimensionId)
                .map(semanticSchemaResp::getDimension)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<String> getDimensionFields(List<String> groupByFields, List<String> whereFields) {
        List<String> dimensionFields = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(groupByFields)) {
            dimensionFields.addAll(groupByFields);
        }
        if (!CollectionUtils.isEmpty(whereFields)) {
            dimensionFields.addAll(whereFields);
        }
        return dimensionFields;
    }

    private List<MetricResp> getMetrics(List<String> metricFields, SemanticSchemaResp semanticSchemaResp) {
        return semanticSchemaResp.getMetrics().stream()
                .filter(metricSchemaResp -> metricFields.contains(metricSchemaResp.getBizName()))
                .collect(Collectors.toList());
    }

    private String convertDimensionIdToBizName(Long id, SemanticSchemaResp semanticSchemaResp) {
        DimSchemaResp dimension = semanticSchemaResp.getDimension(id);
        if (dimension == null) {
            return null;
        }
        return dimension.getBizName();
    }

    private List<DrillDownDimension> getDrillDownDimensions(MetricResp metricResp) {
        return metricService.getDrillDownDimension(metricResp.getId());
    }

}
