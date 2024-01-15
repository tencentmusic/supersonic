package com.tencent.supersonic.headless.api.request;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.headless.api.enums.MetricDefineType;
import com.tencent.supersonic.headless.api.enums.MetricType;
import com.tencent.supersonic.headless.api.pojo.MeasureParam;
import com.tencent.supersonic.headless.api.pojo.MetricDefineByFieldParams;
import com.tencent.supersonic.headless.api.pojo.MetricDefineByMeasureParams;
import com.tencent.supersonic.headless.api.pojo.MetricDefineByMetricParams;
import lombok.Data;
import java.util.List;

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
        if (MetricDefineType.METRIC.equals(metricDefineType)) {
            return MetricType.DERIVED;
        }
        if (MetricDefineType.MEASURE.equals(metricDefineType)) {
            List<MeasureParam> measures = typeParams.getMeasures();
            if (measures.size() > 1) {
                return MetricType.DERIVED;
            }
            if (measures.size() == 1 && measures.get(0).getBizName()
                    .equalsIgnoreCase(typeParams.getExpr())) {
                return MetricType.ATOMIC;
            }
        }
        return MetricType.ATOMIC;
    }

}
