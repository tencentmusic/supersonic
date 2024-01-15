package com.tencent.supersonic.headless.server.utils;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.DataFormat;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.headless.api.enums.MetricDefineType;
import com.tencent.supersonic.headless.api.pojo.MetricDefineByFieldParams;
import com.tencent.supersonic.headless.api.pojo.MetricDefineByMeasureParams;
import com.tencent.supersonic.headless.api.pojo.MetricDefineByMetricParams;
import com.tencent.supersonic.headless.api.pojo.RelateDimension;
import com.tencent.supersonic.headless.api.request.MetricReq;
import com.tencent.supersonic.headless.api.response.MetricResp;
import com.tencent.supersonic.headless.api.response.ModelResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.MetricDO;
import org.springframework.beans.BeanUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetricConverter {

    public static MetricDO convert2MetricDO(MetricReq metricReq) {
        MetricDO metricDO = new MetricDO();
        BeanMapper.mapper(metricReq, metricDO);
        metricDO.setType(metricReq.getMetricType().name());
        metricDO.setTypeParams(metricReq.getTypeParamsJson());
        metricDO.setDataFormat(JSONObject.toJSONString(metricReq.getDataFormat()));
        metricDO.setTags(metricReq.getTag());
        metricDO.setRelateDimensions(JSONObject.toJSONString(metricReq.getRelateDimension()));
        metricDO.setStatus(StatusEnum.ONLINE.getCode());
        metricDO.setExt(JSONObject.toJSONString(metricReq.getExt()));
        metricDO.setDefineType(metricReq.getMetricDefineType().name());
        return metricDO;
    }

    public static MetricDO convert(MetricDO metricDO, MetricReq metricReq) {
        BeanMapper.mapper(metricReq, metricDO);
        metricDO.setDefineType(metricReq.getMetricDefineType().name());
        if (metricReq.getDataFormat() != null) {
            metricDO.setDataFormat(JSONObject.toJSONString(metricReq.getDataFormat()));
        }
        if (metricReq.getRelateDimension() != null) {
            metricDO.setRelateDimensions(JSONObject.toJSONString(metricReq.getRelateDimension()));
        }
        if (metricReq.getTag() != null) {
            metricDO.setTags(metricReq.getTag());
        }
        if (metricReq.getExt() != null) {
            metricDO.setExt(JSONObject.toJSONString(metricReq.getExt()));
        }
        if (metricReq.getTypeParamsJson() != null) {
            metricDO.setTypeParams(metricReq.getTypeParamsJson());
        }
        return metricDO;
    }

    public static MetricResp convert2MetricResp(MetricDO metricDO) {
        return convert2MetricResp(metricDO, new HashMap<>(), Lists.newArrayList());
    }

    public static MetricResp convert2MetricResp(MetricDO metricDO, Map<Long, ModelResp> modelMap, List<Long> collect) {
        MetricResp metricResp = new MetricResp();
        BeanUtils.copyProperties(metricDO, metricResp);

        metricResp.setDataFormat(JSONObject.parseObject(metricDO.getDataFormat(), DataFormat.class));
        ModelResp modelResp = modelMap.get(metricDO.getModelId());
        if (modelResp != null) {
            metricResp.setModelName(modelResp.getName());
            metricResp.setDomainId(modelResp.getDomainId());
        }
        metricResp.setIsCollect(collect != null && collect.contains(metricDO.getId()));
        metricResp.setTag(metricDO.getTags());
        metricResp.setRelateDimension(JSONObject.parseObject(metricDO.getRelateDimensions(),
                RelateDimension.class));
        if (metricDO.getExt() != null) {
            metricResp.setExt(JSONObject.parseObject(metricDO.getExt(), Map.class));
        }
        metricResp.setTypeEnum(TypeEnums.METRIC);
        if (MetricDefineType.MEASURE.name().equalsIgnoreCase(metricDO.getDefineType())) {
            metricResp.setTypeParams(JSONObject.parseObject(metricDO.getTypeParams(),
                    MetricDefineByMeasureParams.class));
        } else if (MetricDefineType.METRIC.name().equalsIgnoreCase(metricDO.getDefineType())) {
            metricResp.setMetricDefineByMetricParams(JSONObject.parseObject(metricDO.getTypeParams(),
                    MetricDefineByMetricParams.class));
        } else if (MetricDefineType.FIELD.name().equalsIgnoreCase(metricDO.getDefineType())) {
            metricResp.setMetricDefineByFieldParams(JSONObject.parseObject(metricDO.getTypeParams(),
                    MetricDefineByFieldParams.class));
        }
        if (metricDO.getDefineType() != null) {
            metricResp.setMetricDefineType(MetricDefineType.valueOf(metricDO.getDefineType()));
        }
        return metricResp;
    }

}
