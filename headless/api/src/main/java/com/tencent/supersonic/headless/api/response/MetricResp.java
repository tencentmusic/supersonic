package com.tencent.supersonic.headless.api.response;


import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.DataFormat;
import com.tencent.supersonic.headless.api.enums.MetricDefineType;
import com.tencent.supersonic.headless.api.pojo.DrillDownDimension;
import com.tencent.supersonic.headless.api.pojo.MetricDefineByFieldParams;
import com.tencent.supersonic.headless.api.pojo.MetricDefineByMeasureParams;
import com.tencent.supersonic.headless.api.pojo.MetricDefineByMetricParams;
import com.tencent.supersonic.headless.api.pojo.RelateDimension;
import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Data
@ToString(callSuper = true)
public class MetricResp extends SchemaItem {

    private Long modelId;

    private Long domainId;

    private String modelName;

    //ATOMIC DERIVED
    private String type;

    private String dataFormatType;

    private DataFormat dataFormat;

    private String alias;

    private List<String> tags;

    private RelateDimension relateDimension;

    private boolean hasAdminRes = false;

    private Boolean isCollect;

    private Map<String, Object> ext = new HashMap<>();

    private MetricDefineType metricDefineType = MetricDefineType.MEASURE;

    private MetricDefineByMeasureParams metricDefineByMeasureParams;

    private MetricDefineByFieldParams metricDefineByFieldParams;

    private MetricDefineByMetricParams metricDefineByMetricParams;

    public void setTag(String tag) {
        if (StringUtils.isBlank(tag)) {
            tags = Lists.newArrayList();
        } else {
            tags = Arrays.asList(tag.split(","));
        }
    }

    public String getRelaDimensionIdKey() {
        if (relateDimension == null || CollectionUtils.isEmpty(relateDimension.getDrillDownDimensions())) {
            return "";
        }
        return relateDimension.getDrillDownDimensions().stream()
                .map(DrillDownDimension::getDimensionId)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    public String getDefaultAgg() {
        if (metricDefineByMeasureParams != null
                && CollectionUtils.isNotEmpty(metricDefineByMeasureParams.getMeasures())) {
            return metricDefineByMeasureParams.getMeasures().get(0).getAgg();
        }
        return "";
    }

    public String getExpr() {
        if (MetricDefineType.MEASURE.equals(metricDefineType)) {
            return metricDefineByMeasureParams.getExpr();
        } else if (MetricDefineType.METRIC.equals(metricDefineType)) {
            return metricDefineByMetricParams.getExpr();
        } else if (MetricDefineType.FIELD.equals(metricDefineType)) {
            return metricDefineByFieldParams.getExpr();
        }
        return "";
    }

}
