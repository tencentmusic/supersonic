package com.tencent.supersonic.headless.api.pojo.enums;

import com.tencent.supersonic.headless.api.pojo.Measure;
import com.tencent.supersonic.headless.api.pojo.MetricDefineByMeasureParams;

import java.util.List;
import java.util.Objects;

public enum MetricType {
    ATOMIC, DERIVED;

    public static MetricType of(String src) {
        for (MetricType metricType : MetricType.values()) {
            if (Objects.nonNull(src) && src.equalsIgnoreCase(metricType.name())) {
                return metricType;
            }
        }
        return null;
    }

    public static Boolean isDerived(MetricDefineType metricDefineType,
            MetricDefineByMeasureParams typeParams) {
        if (MetricDefineType.METRIC.equals(metricDefineType)) {
            return true;
        }
        if (MetricDefineType.FIELD.equals(metricDefineType)) {
            return true;
        }
        if (MetricDefineType.MEASURE.equals(metricDefineType)) {
            List<Measure> measures = typeParams.getMeasures();
            if (measures.size() > 1) {
                return true;
            }
            if (measures.size() == 1
                    && measures.get(0).getBizName().equalsIgnoreCase(typeParams.getExpr())) {
                return false;
            }
        }
        return false;
    }
}
