package com.tencent.supersonic.semantic.api.core.request;


import com.tencent.supersonic.semantic.api.core.enums.MetricTypeEnum;
import com.tencent.supersonic.semantic.api.core.pojo.Measure;
import com.tencent.supersonic.semantic.api.core.pojo.MetricTypeParams;
import lombok.Data;
import java.util.List;

@Data
public class MetricReq extends MetricBaseReq {

    private MetricTypeEnum metricType;

    private MetricTypeParams typeParams;

    public MetricTypeEnum getMetricType() {
        if (metricType != null) {
            return metricType;
        }
        List<Measure> measureList = typeParams.getMeasures();
        if (measureList.size() > 1) {
            return MetricTypeEnum.DERIVED;
        }
        if (measureList.size() == 1 && typeParams.getExpr().trim().equalsIgnoreCase(measureList.get(0).getBizName())) {
            return MetricTypeEnum.ATOMIC;
        }
        throw new RuntimeException("measure can not be none");
    }

}
