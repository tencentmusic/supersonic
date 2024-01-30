package com.tencent.supersonic.headless.server.manager;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.enums.MetricDefineType;
import com.tencent.supersonic.headless.api.pojo.FieldParam;
import com.tencent.supersonic.headless.api.pojo.MeasureParam;
import com.tencent.supersonic.headless.api.pojo.MetricDefineByFieldParams;
import com.tencent.supersonic.headless.api.pojo.MetricDefineByMeasureParams;
import com.tencent.supersonic.headless.api.pojo.MetricDefineByMetricParams;
import com.tencent.supersonic.headless.api.pojo.MetricParam;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.server.pojo.yaml.FieldParamYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.MeasureYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.MetricParamYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.MetricTypeParamsYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.MetricYamlTpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * manager to handle the metric
 */
@Slf4j
@Service
public class MetricYamlManager {

    public static List<MetricYamlTpl> convert2YamlObj(List<MetricResp> metrics) {

        List<MetricYamlTpl> metricYamlTpls = new ArrayList<>();
        for (MetricResp metric : metrics) {
            MetricYamlTpl metricYamlTpl = convert2MetricYamlTpl(metric);
            metricYamlTpls.add(metricYamlTpl);
        }
        return metricYamlTpls;
    }

    public static MetricYamlTpl convert2MetricYamlTpl(MetricResp metric) {
        MetricYamlTpl metricYamlTpl = new MetricYamlTpl();
        BeanUtils.copyProperties(metric, metricYamlTpl);
        metricYamlTpl.setName(metric.getBizName());
        metricYamlTpl.setOwners(Lists.newArrayList(metric.getCreatedBy()));
        MetricTypeParamsYamlTpl metricTypeParamsYamlTpl = new MetricTypeParamsYamlTpl();
        if (MetricDefineType.MEASURE.equals(metric.getMetricDefineType())) {
            MetricDefineByMeasureParams metricDefineParams = metric.getMetricDefineByMeasureParams();
            metricTypeParamsYamlTpl.setExpr(metricDefineParams.getExpr());
            List<MeasureParam> measures = metricDefineParams.getMeasures();
            metricTypeParamsYamlTpl.setMeasures(
                    measures.stream().map(MetricYamlManager::convert).collect(Collectors.toList()));
        } else if (MetricDefineType.FIELD.equals(metric.getMetricDefineType())) {
            MetricDefineByFieldParams metricDefineParams = metric.getMetricDefineByFieldParams();
            metricTypeParamsYamlTpl.setExpr(metricDefineParams.getExpr());
            List<FieldParam> fields = metricDefineParams.getFields();
            metricTypeParamsYamlTpl.setFields(
                    fields.stream().map(MetricYamlManager::convert).collect(Collectors.toList()));
        } else if (MetricDefineType.METRIC.equals(metric.getMetricDefineType())) {
            MetricDefineByMetricParams metricDefineByMetricParams = metric.getMetricDefineByMetricParams();
            metricTypeParamsYamlTpl.setExpr(metricDefineByMetricParams.getExpr());
            List<MetricParam> metrics = metricDefineByMetricParams.getMetrics();
            metricTypeParamsYamlTpl.setMetrics(
                    metrics.stream().map(MetricYamlManager::convert).collect(Collectors.toList()));
        }
        metricYamlTpl.setTypeParams(metricTypeParamsYamlTpl);
        return metricYamlTpl;
    }

    public static MeasureYamlTpl convert(MeasureParam measure) {
        MeasureYamlTpl measureYamlTpl = new MeasureYamlTpl();
        measureYamlTpl.setName(measure.getBizName());
        measureYamlTpl.setConstraint(measure.getConstraint());
        measureYamlTpl.setAgg(measure.getAgg());
        return measureYamlTpl;
    }

    public static FieldParamYamlTpl convert(FieldParam fieldParam) {
        FieldParamYamlTpl fieldParamYamlTpl = new FieldParamYamlTpl();
        fieldParamYamlTpl.setFieldName(fieldParam.getFieldName());
        return fieldParamYamlTpl;
    }

    public static MetricParamYamlTpl convert(MetricParam metricParam) {
        MetricParamYamlTpl metricParamYamlTpl = new MetricParamYamlTpl();
        metricParamYamlTpl.setBizName(metricParam.getBizName());
        metricParamYamlTpl.setId(metricParam.getId());
        return metricParamYamlTpl;
    }

}
