package com.tencent.supersonic.headless.core.manager;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.MeasureParam;
import com.tencent.supersonic.headless.api.pojo.MetricDefineByMeasureParams;
import com.tencent.supersonic.headless.api.response.MetricResp;
import com.tencent.supersonic.headless.core.pojo.yaml.MeasureYamlTpl;
import com.tencent.supersonic.headless.core.pojo.yaml.MetricTypeParamsYamlTpl;
import com.tencent.supersonic.headless.core.pojo.yaml.MetricYamlTpl;
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
        MetricDefineByMeasureParams metricDefineParams = metric.getTypeParams();
        MetricTypeParamsYamlTpl metricTypeParamsYamlTpl = new MetricTypeParamsYamlTpl();
        metricTypeParamsYamlTpl.setExpr(metricDefineParams.getExpr());
        List<MeasureParam> measures = metricDefineParams.getMeasures();
        metricTypeParamsYamlTpl.setMeasures(
                measures.stream().map(MetricYamlManager::convert).collect(Collectors.toList()));
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

}
