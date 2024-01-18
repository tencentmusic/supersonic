package com.tencent.supersonic.headless.api.request;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.headless.api.enums.MetricDefineType;
import com.tencent.supersonic.headless.api.enums.MetricType;
import com.tencent.supersonic.headless.api.pojo.MetricDefineByFieldParams;
import com.tencent.supersonic.headless.api.pojo.MetricDefineByMeasureParams;
import com.tencent.supersonic.headless.api.pojo.MetricDefineByMetricParams;
import lombok.Data;

@Data
public class MetricReq extends MetricBaseReq {

    private MetricDefineType metricDefineType = MetricDefineType.MEASURE;
    private MetricDefineByMeasureParams typeParams;
    private MetricDefineByFieldParams metricDefineByFieldParams;
    private MetricDefineByMetricParams metricDefineByMetricParams;

    public String getTypeParamsJson() {
        if (metricDefineByFieldParams != null) {
            return JSONObject.toJSONString(metricDefineByFieldParams);
        } else if (typeParams != null) {
            return JSONObject.toJSONString(typeParams);
        } else if (metricDefineByMetricParams != null) {
            return JSONObject.toJSONString(metricDefineByMetricParams);
        }
        return null;
    }

    public MetricType getMetricType() {
        return MetricType.isDerived(metricDefineType, typeParams) ? MetricType.DERIVED : MetricType.ATOMIC;
    }

}
