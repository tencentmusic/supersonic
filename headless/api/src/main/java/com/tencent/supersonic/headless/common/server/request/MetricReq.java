package com.tencent.supersonic.headless.common.server.request;


import com.tencent.supersonic.headless.common.server.enums.MetricType;
import com.tencent.supersonic.headless.common.server.pojo.Measure;
import com.tencent.supersonic.headless.common.server.pojo.MetricTypeParams;
import lombok.Data;
import java.util.List;

@Data
public class MetricReq extends MetricBaseReq {

    private MetricType metricType;

    private MetricTypeParams typeParams;

    public MetricType getMetricType() {
        if (metricType != null) {
            return metricType;
        }
        List<Measure> measureList = typeParams.getMeasures();
        if (measureList.size() == 1 && typeParams.getExpr().trim().equalsIgnoreCase(measureList.get(0).getBizName())) {
            return MetricType.ATOMIC;
        } else if (measureList.size() >= 1) {
            return MetricType.DERIVED;
        }
        throw new RuntimeException("measure can not be none");
    }

}
