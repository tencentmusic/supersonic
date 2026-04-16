package com.tencent.supersonic.headless.server.manager;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.*;
import com.tencent.supersonic.headless.api.pojo.enums.MetricDefineType;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.schema.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** manager to handle the metric */
@Slf4j
@Service
public class MetricSchemaManager {

    public static List<MetricSchema> convertToSchema(List<MetricResp> metrics) {

        List<MetricSchema> metricSchemas = new ArrayList<>();
        for (MetricResp metric : metrics) {
            MetricSchema metricSchema = convertToSchema(metric);
            metricSchemas.add(metricSchema);
        }
        return metricSchemas;
    }

    public static MetricSchema convertToSchema(MetricResp metric) {
        MetricSchema metricSchema = new MetricSchema();
        BeanUtils.copyProperties(metric, metricSchema);
        metricSchema.setName(metric.getBizName());
        metricSchema.setOwners(Lists.newArrayList(metric.getCreatedBy()));
        MetricTypeParamsSchema metricTypeParamsSchema = new MetricTypeParamsSchema();
        if (MetricDefineType.MEASURE.equals(metric.getMetricDefineType())) {
            MetricDefineByMeasureParams metricDefineParams =
                    metric.getMetricDefineByMeasureParams();
            metricTypeParamsSchema.setExpr(metricDefineParams.getExpr());
            List<Measure> measures = metricDefineParams.getMeasures();
            metricTypeParamsSchema.setMeasures(measures.stream().map(MetricSchemaManager::convert)
                    .collect(Collectors.toList()));
        } else if (MetricDefineType.FIELD.equals(metric.getMetricDefineType())) {
            MetricDefineByFieldParams metricDefineParams = metric.getMetricDefineByFieldParams();
            metricTypeParamsSchema.setExpr(metricDefineParams.getExpr());
            List<FieldParam> fields = metricDefineParams.getFields();
            metricTypeParamsSchema.setFields(
                    fields.stream().map(MetricSchemaManager::convert).collect(Collectors.toList()));
        } else if (MetricDefineType.METRIC.equals(metric.getMetricDefineType())) {
            MetricDefineByMetricParams metricDefineByMetricParams =
                    metric.getMetricDefineByMetricParams();
            metricTypeParamsSchema.setExpr(metricDefineByMetricParams.getExpr());
            List<MetricParam> metrics = metricDefineByMetricParams.getMetrics();
            metricTypeParamsSchema.setMetrics(metrics.stream().map(MetricSchemaManager::convert)
                    .collect(Collectors.toList()));
        }
        metricSchema.setTypeParams(metricTypeParamsSchema);
        return metricSchema;
    }

    public static MeasureSchema convert(Measure measure) {
        MeasureSchema measureSchema = new MeasureSchema();
        measureSchema.setName(measure.getBizName());
        measureSchema.setConstraint(measure.getConstraint());
        measureSchema.setAgg(measure.getAgg());
        return measureSchema;
    }

    public static FieldParamSchema convert(FieldParam fieldParam) {
        FieldParamSchema fieldParamSchema = new FieldParamSchema();
        fieldParamSchema.setFieldName(fieldParam.getFieldName());
        return fieldParamSchema;
    }

    public static MetricParamSchema convert(MetricParam metricParam) {
        MetricParamSchema metricParamSchema = new MetricParamSchema();
        metricParamSchema.setBizName(metricParam.getBizName());
        metricParamSchema.setId(metricParam.getId());
        return metricParamSchema;
    }
}
