package com.tencent.supersonic.semantic.model.domain.utils;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.DataFormat;
import com.tencent.supersonic.semantic.api.model.pojo.Measure;
import com.tencent.supersonic.semantic.api.model.pojo.MetricTypeParams;
import com.tencent.supersonic.semantic.api.model.pojo.RelateDimension;
import com.tencent.supersonic.semantic.api.model.response.ModelResp;
import com.tencent.supersonic.semantic.api.model.yaml.MeasureYamlTpl;
import com.tencent.supersonic.semantic.api.model.yaml.MetricTypeParamsYamlTpl;
import com.tencent.supersonic.semantic.api.model.yaml.MetricYamlTpl;
import com.tencent.supersonic.semantic.api.model.request.MetricReq;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.semantic.model.domain.dataobject.MetricDO;
import com.tencent.supersonic.semantic.model.domain.pojo.Metric;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;

public class MetricConverter {

    public static Metric convert(MetricReq metricReq) {
        Metric metric = new Metric();
        BeanUtils.copyProperties(metricReq, metric);
        metric.setType(metricReq.getMetricType().name());
        metric.setTypeParams(metricReq.getTypeParams());
        return metric;
    }

    public static MetricDO convert(MetricDO metricDO, Metric metric) {
        BeanMapper.mapper(metric, metricDO);
        metricDO.setTypeParams(JSONObject.toJSONString(metric.getTypeParams()));
        if (metric.getDataFormat() != null) {
            metricDO.setDataFormat(JSONObject.toJSONString(metric.getDataFormat()));
        }
        if (metric.getRelateDimension() != null) {
            metricDO.setRelateDimensions(JSONObject.toJSONString(metric.getRelateDimension()));
        }
        metricDO.setTags(metric.getTag());
        return metricDO;
    }

    public static MeasureYamlTpl convert(Measure measure) {
        MeasureYamlTpl measureYamlTpl = new MeasureYamlTpl();
        measureYamlTpl.setName(measure.getBizName());
        return measureYamlTpl;
    }

    public static MetricDO convert2MetricDO(Metric metric) {
        MetricDO metricDO = new MetricDO();
        BeanUtils.copyProperties(metric, metricDO);
        metricDO.setTypeParams(JSONObject.toJSONString(metric.getTypeParams()));
        metricDO.setDataFormat(JSONObject.toJSONString(metric.getDataFormat()));
        metricDO.setTags(metric.getTag());
        metricDO.setRelateDimensions(JSONObject.toJSONString(metric.getRelateDimension()));
        return metricDO;
    }


    public static MetricResp convert2MetricResp(MetricDO metricDO, Map<Long, ModelResp> modelMap) {
        MetricResp metricResp = new MetricResp();
        BeanUtils.copyProperties(metricDO, metricResp);
        metricResp.setTypeParams(JSONObject.parseObject(metricDO.getTypeParams(), MetricTypeParams.class));
        metricResp.setDataFormat(JSONObject.parseObject(metricDO.getDataFormat(), DataFormat.class));
        ModelResp modelResp = modelMap.get(metricDO.getModelId());
        if (modelResp != null) {
            metricResp.setModelName(modelResp.getName());
            metricResp.setDomainId(modelResp.getDomainId());
        }
        metricResp.setTag(metricDO.getTags());
        metricResp.setRelateDimension(JSONObject.parseObject(metricDO.getRelateDimensions(),
                RelateDimension.class));
        return metricResp;
    }
    public static Metric convert2Metric(MetricDO metricDO) {
        Metric metric = new Metric();
        BeanUtils.copyProperties(metricDO, metric);
        metric.setTypeParams(JSONObject.parseObject(metricDO.getTypeParams(), MetricTypeParams.class));
        return metric;
    }

    public static MetricYamlTpl convert2MetricYamlTpl(Metric metric) {
        MetricYamlTpl metricYamlTpl = new MetricYamlTpl();
        BeanUtils.copyProperties(metric, metricYamlTpl);
        metricYamlTpl.setName(metric.getBizName());
        metricYamlTpl.setOwners(Lists.newArrayList(metric.getCreatedBy()));
        MetricTypeParams exprMetricTypeParams = metric.getTypeParams();
        MetricTypeParamsYamlTpl metricTypeParamsYamlTpl = new MetricTypeParamsYamlTpl();
        metricTypeParamsYamlTpl.setExpr(exprMetricTypeParams.getExpr());
        List<Measure> measures = exprMetricTypeParams.getMeasures();
        metricTypeParamsYamlTpl.setMeasures(
                measures.stream().map(MetricConverter::convert).collect(Collectors.toList()));
        metricYamlTpl.setTypeParams(metricTypeParamsYamlTpl);
        return metricYamlTpl;
    }

    public static List<Metric> metricInfo2Metric(List<MetricResp> metricDescs) {
        List<Metric> metrics = new ArrayList<>();
        for (MetricResp metricDesc : metricDescs) {
            Metric metric = new Metric();
            BeanUtils.copyProperties(metricDesc, metric);
            metrics.add(metric);
        }
        return metrics;
    }

}
