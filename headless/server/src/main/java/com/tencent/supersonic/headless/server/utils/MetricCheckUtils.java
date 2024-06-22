package com.tencent.supersonic.headless.server.utils;

import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import com.tencent.supersonic.common.jsqlparser.SqlSelectFunctionHelper;
import com.tencent.supersonic.headless.api.pojo.enums.MetricDefineType;
import com.tencent.supersonic.headless.api.pojo.MetricDefineByFieldParams;
import com.tencent.supersonic.headless.api.pojo.MetricDefineByMeasureParams;
import com.tencent.supersonic.headless.api.pojo.MetricDefineByMetricParams;
import com.tencent.supersonic.headless.api.pojo.request.MetricReq;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

public class MetricCheckUtils {

    public static void checkParam(MetricReq metricReq) {
        String expr = "";
        if (MetricDefineType.METRIC.equals(metricReq.getMetricDefineType())) {
            MetricDefineByMetricParams typeParams = metricReq.getMetricDefineByMetricParams();
            if (typeParams == null) {
                throw new InvalidArgumentException("指标定义参数不可为空");
            }
            expr = typeParams.getExpr();
            if (CollectionUtils.isEmpty(typeParams.getMetrics())) {
                throw new InvalidArgumentException("定义指标的指标列表参数不可为空");
            }
            if (hasAggregateFunction(expr)) {
                throw new InvalidArgumentException("基于指标来创建指标,表达式中不可再包含聚合函数");
            }
        }
        if (MetricDefineType.MEASURE.equals(metricReq.getMetricDefineType())) {
            MetricDefineByMeasureParams typeParams = metricReq.getMetricDefineByMeasureParams();
            if (typeParams == null) {
                throw new InvalidArgumentException("指标定义参数不可为空");
            }
            expr = typeParams.getExpr();
            if (CollectionUtils.isEmpty(typeParams.getMeasures())) {
                throw new InvalidArgumentException("定义指标的度量列表参数不可为空");
            }
            if (hasAggregateFunction(expr)) {
                throw new InvalidArgumentException("基于度量来创建指标,表达式中不可再包含聚合函数");
            }
        }
        if (MetricDefineType.FIELD.equals(metricReq.getMetricDefineType())) {
            MetricDefineByFieldParams typeParams = metricReq.getMetricDefineByFieldParams();
            if (typeParams == null) {
                throw new InvalidArgumentException("指标定义参数不可为空");
            }
            expr = typeParams.getExpr();
            if (CollectionUtils.isEmpty(typeParams.getFields())) {
                throw new InvalidArgumentException("定义指标的字段列表参数不可为空");
            }
            if (!hasAggregateFunction(expr)) {
                throw new InvalidArgumentException("基于字段来创建指标,表达式中必须包含聚合函数");
            }
        }
        if (StringUtils.isBlank(expr)) {
            throw new InvalidArgumentException("表达式不可为空");
        }
        String forbiddenCharacters = NameCheckUtils.findForbiddenCharacters(metricReq.getName());
        if (StringUtils.isNotBlank(forbiddenCharacters)) {
            throw new InvalidArgumentException(String.format("名称包含特殊字符%s, 请修改", forbiddenCharacters));
        }
    }

    private static boolean hasAggregateFunction(String expr) {
        String sql = String.format("select %s from table", expr);
        return SqlSelectFunctionHelper.hasAggregateFunction(sql);
    }

}
