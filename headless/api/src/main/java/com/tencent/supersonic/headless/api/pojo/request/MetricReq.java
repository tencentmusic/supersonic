package com.tencent.supersonic.headless.api.pojo.request;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.headless.api.pojo.MetricDefineByFieldParams;
import com.tencent.supersonic.headless.api.pojo.MetricDefineByMeasureParams;
import com.tencent.supersonic.headless.api.pojo.MetricDefineByMetricParams;
import com.tencent.supersonic.headless.api.pojo.enums.MetricDefineType;
import com.tencent.supersonic.headless.api.pojo.enums.MetricType;
import lombok.Data;

@Data
public class MetricReq extends MetricBaseReq {

    private MetricDefineType metricDefineType = MetricDefineType.MEASURE;
    private MetricDefineByMeasureParams metricDefineByMeasureParams;
    private MetricDefineByFieldParams metricDefineByFieldParams;
    private MetricDefineByMetricParams metricDefineByMetricParams;

    public String getTypeParamsJson() {
        if (MetricDefineType.FIELD.equals(metricDefineType) && metricDefineByFieldParams != null) {
            return JSONObject.toJSONString(metricDefineByFieldParams);
        } else if (MetricDefineType.MEASURE.equals(metricDefineType)
                && metricDefineByMeasureParams != null) {
            return JSONObject.toJSONString(metricDefineByMeasureParams);
        } else if (MetricDefineType.METRIC.equals(metricDefineType)
                && metricDefineByMetricParams != null) {
            return JSONObject.toJSONString(metricDefineByMetricParams);
        }
        return null;
    }

    public MetricType getMetricType() {
        return MetricType.isDerived(metricDefineType, metricDefineByMeasureParams)
                ? MetricType.DERIVED
                : MetricType.ATOMIC;
    }
}
