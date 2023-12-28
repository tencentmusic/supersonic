package com.tencent.supersonic.headless.model.domain.utils;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.DataFormat;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.headless.common.model.pojo.Measure;
import com.tencent.supersonic.headless.common.model.pojo.MetricTypeParams;
import com.tencent.supersonic.headless.common.model.pojo.RelateDimension;
import com.tencent.supersonic.headless.common.model.request.MetricReq;
import com.tencent.supersonic.headless.common.model.response.MetricResp;
import com.tencent.supersonic.headless.common.model.response.ModelResp;
import com.tencent.supersonic.headless.common.model.yaml.MeasureYamlTpl;
import com.tencent.supersonic.headless.common.model.yaml.MetricTypeParamsYamlTpl;
import com.tencent.supersonic.headless.common.model.yaml.MetricYamlTpl;
import com.tencent.supersonic.headless.model.domain.dataobject.MetricDO;
import com.tencent.supersonic.headless.model.domain.pojo.Metric;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MetricConverter {

    public static MetricDO convert2MetricDO(MetricReq metricReq) {
        MetricDO metricDO = new MetricDO();
        BeanMapper.mapper(metricReq, metricDO);
        metricDO.setType(metricReq.getMetricType().name());
        metricDO.setTypeParams(JSONObject.toJSONString(metricReq.getTypeParams()));
        metricDO.setDataFormat(JSONObject.toJSONString(metricReq.getDataFormat()));
        metricDO.setTags(metricReq.getTag());
        metricDO.setRelateDimensions(JSONObject.toJSONString(metricReq.getRelateDimension()));
        metricDO.setStatus(StatusEnum.ONLINE.getCode());
        metricDO.setExt(JSONObject.toJSONString(metricReq.getExt()));
        return metricDO;
    }

    public static MetricDO convert(MetricDO metricDO, MetricReq metricReq) {
        BeanMapper.mapper(metricReq, metricDO);
        metricDO.setTypeParams(JSONObject.toJSONString(metricReq.getTypeParams()));
        if (metricReq.getDataFormat() != null) {
            metricDO.setDataFormat(JSONObject.toJSONString(metricReq.getDataFormat()));
        }
        if (metricReq.getRelateDimension() != null) {
            metricDO.setRelateDimensions(JSONObject.toJSONString(metricReq.getRelateDimension()));
        }
        metricDO.setTags(metricReq.getTag());
        metricDO.setExt(JSONObject.toJSONString(metricReq.getExt()));
        return metricDO;
    }

    public static MeasureYamlTpl convert(Measure measure) {
        MeasureYamlTpl measureYamlTpl = new MeasureYamlTpl();
        measureYamlTpl.setName(measure.getBizName());
        return measureYamlTpl;
    }

    public static MetricResp convert2MetricResp(MetricDO metricDO, Map<Long, ModelResp> modelMap, List<Long> collect) {
        MetricResp metricResp = new MetricResp();
        BeanUtils.copyProperties(metricDO, metricResp);
        metricResp.setTypeParams(JSONObject.parseObject(metricDO.getTypeParams(), MetricTypeParams.class));
        metricResp.setDataFormat(JSONObject.parseObject(metricDO.getDataFormat(), DataFormat.class));
        ModelResp modelResp = modelMap.get(metricDO.getModelId());
        if (modelResp != null) {
            metricResp.setModelName(modelResp.getName());
            metricResp.setDomainId(modelResp.getDomainId());
        }
        if (collect != null && collect.contains(metricDO.getId())) {
            metricResp.setIsCollect(true);
        } else {
            metricResp.setIsCollect(false);
        }
        metricResp.setTag(metricDO.getTags());
        metricResp.setRelateDimension(JSONObject.parseObject(metricDO.getRelateDimensions(),
                RelateDimension.class));
        if (metricDO.getExt() != null) {
            metricResp.setExt(JSONObject.parseObject(metricDO.getExt(), Map.class));
        }
        return metricResp;
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
