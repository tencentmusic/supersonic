package com.tencent.supersonic.headless.api.pojo.response;


import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.DataFormat;
import com.tencent.supersonic.headless.api.pojo.DrillDownDimension;
import com.tencent.supersonic.headless.api.pojo.RelateDimension;
import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import com.tencent.supersonic.headless.api.pojo.enums.MetricDefineType;
import com.tencent.supersonic.headless.api.pojo.MetricDefineByFieldParams;
import com.tencent.supersonic.headless.api.pojo.MetricDefineByMeasureParams;
import com.tencent.supersonic.headless.api.pojo.MetricDefineByMetricParams;
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

    private String modelBizName;

    private String modelName;

    //ATOMIC DERIVED
    private String type;

    private String dataFormatType;

    private DataFormat dataFormat;

    private String alias;

    private List<String> classifications;

    private RelateDimension relateDimension;

    private boolean hasAdminRes = false;

    private Boolean isCollect;

    private Map<String, Object> ext = new HashMap<>();

    private MetricDefineType metricDefineType = MetricDefineType.MEASURE;

    private MetricDefineByMeasureParams metricDefineByMeasureParams;

    private MetricDefineByFieldParams metricDefineByFieldParams;

    private MetricDefineByMetricParams metricDefineByMetricParams;

    private int isTag;

    private Integer isPublish;

    private double similarity;

    private String defaultAgg;

    public void setClassifications(String tag) {
        if (StringUtils.isBlank(tag)) {
            classifications = Lists.newArrayList();
        } else {
            classifications = Arrays.asList(tag.split(","));
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

    public List<DrillDownDimension> getDrillDownDimensions() {
        if (relateDimension == null || CollectionUtils.isEmpty(relateDimension.getDrillDownDimensions())) {
            return Lists.newArrayList();
        }
        return relateDimension.getDrillDownDimensions();
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
