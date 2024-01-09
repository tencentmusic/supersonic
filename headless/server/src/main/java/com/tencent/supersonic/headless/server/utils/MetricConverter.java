package com.tencent.supersonic.headless.server.utils;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.common.pojo.DataFormat;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.common.util.BeanMapper;
import com.tencent.supersonic.headless.api.pojo.MetricTypeParams;
import com.tencent.supersonic.headless.api.pojo.RelateDimension;
import com.tencent.supersonic.headless.api.request.MetricReq;
import com.tencent.supersonic.headless.api.response.MetricResp;
import com.tencent.supersonic.headless.api.response.ModelResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.MetricDO;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.Map;

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
        metricResp.setTypeEnum(TypeEnums.METRIC);
        return metricResp;
    }

}
