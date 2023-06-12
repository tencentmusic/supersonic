package com.tencent.supersonic.semantic.core.domain.utils;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.semantic.api.core.enums.MetricTypeEnum;
import com.tencent.supersonic.semantic.api.core.pojo.DataFormat;
import com.tencent.supersonic.semantic.api.core.pojo.Measure;
import com.tencent.supersonic.semantic.api.core.pojo.MetricTypeParams;
import com.tencent.supersonic.semantic.api.core.pojo.yaml.MeasureYamlTpl;
import com.tencent.supersonic.semantic.api.core.pojo.yaml.MetricTypeParamsYamlTpl;
import com.tencent.supersonic.semantic.api.core.pojo.yaml.MetricYamlTpl;
import com.tencent.supersonic.semantic.api.core.request.MetricReq;
import com.tencent.supersonic.semantic.api.core.response.DomainResp;
import com.tencent.supersonic.semantic.api.core.response.MetricResp;
import com.tencent.supersonic.common.util.mapper.BeanMapper;
import com.tencent.supersonic.semantic.core.domain.dataobject.MetricDO;
import com.tencent.supersonic.semantic.core.domain.pojo.Metric;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;

public class MetricConverter {

    public static Metric convert(MetricReq metricReq) {
        Metric metric = new Metric();
        BeanUtils.copyProperties(metricReq, metric);
        metric.setType(MetricTypeEnum.EXPR.getName());
        metric.setTypeParams(metricReq.getTypeParams());
        return metric;
    }

    public static MetricDO convert(MetricDO metricDO, Metric metric) {
        BeanMapper.mapper(metric, metricDO);
        metricDO.setTypeParams(JSONObject.toJSONString(metric.getTypeParams()));
        if (metric.getDataFormat() != null) {
            metricDO.setDataFormat(JSONObject.toJSONString(metric.getDataFormat()));
        }
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
        return metricDO;
    }


    public static MetricResp convert2MetricDesc(MetricDO metricDO, Map<Long, DomainResp> domainMap) {
        MetricResp metricDesc = new MetricResp();
        BeanUtils.copyProperties(metricDO, metricDesc);
        metricDesc.setTypeParams(JSONObject.parseObject(metricDO.getTypeParams(), MetricTypeParams.class));
        metricDesc.setDataFormat(JSONObject.parseObject(metricDO.getDataFormat(), DataFormat.class));
        DomainResp domainResp = domainMap.get(metricDO.getDomainId());
        if (domainResp != null) {
            metricDesc.setFullPath(domainMap.get(metricDO.getDomainId()).getFullPath() + metricDO.getBizName());
            metricDesc.setDomainName(domainMap.get(metricDO.getDomainId()).getName());
        }
        return metricDesc;
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
