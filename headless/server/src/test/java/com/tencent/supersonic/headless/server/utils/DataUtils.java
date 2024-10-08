package com.tencent.supersonic.headless.server.utils;

import com.tencent.supersonic.headless.api.pojo.DrillDownDimension;
import com.tencent.supersonic.headless.api.pojo.RelateDimension;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;

import java.util.List;

public class DataUtils {

    public static DimSchemaResp mockDimension(Long id, String bizName, String name) {
        DimSchemaResp dimSchemaResp = new DimSchemaResp();
        dimSchemaResp.setId(id);
        dimSchemaResp.setBizName(bizName);
        dimSchemaResp.setName(name);
        return dimSchemaResp;
    }

    public static MetricSchemaResp mockMetric(Long id, String bizName) {
        MetricSchemaResp metricSchemaResp = new MetricSchemaResp();
        metricSchemaResp.setId(id);
        metricSchemaResp.setBizName(bizName);
        RelateDimension relateDimension = new RelateDimension();
        metricSchemaResp.setRelateDimension(relateDimension);
        return metricSchemaResp;
    }

    public static MetricSchemaResp mockMetric(Long id, String bizName, String name,
            List<DrillDownDimension> drillDownDimensions) {
        MetricSchemaResp metricSchemaResp = new MetricSchemaResp();
        metricSchemaResp.setId(id);
        metricSchemaResp.setName(name);
        metricSchemaResp.setBizName(bizName);
        metricSchemaResp.setRelateDimension(
                RelateDimension.builder().drillDownDimensions(drillDownDimensions).build());
        return metricSchemaResp;
    }

    public static MetricSchemaResp mockMetric(Long id, String bizName,
            List<DrillDownDimension> drillDownDimensions) {
        return mockMetric(id, bizName, null, drillDownDimensions);
    }
}
