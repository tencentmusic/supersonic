package com.tencent.supersonic.headless.server.service;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.DataItem;
import com.tencent.supersonic.common.pojo.enums.EventType;
import com.tencent.supersonic.headless.api.pojo.DrillDownDimension;
import com.tencent.supersonic.headless.api.pojo.MetricQueryDefaultConfig;
import com.tencent.supersonic.headless.api.request.MetaBatchReq;
import com.tencent.supersonic.headless.api.request.MetricReq;
import com.tencent.supersonic.headless.api.request.PageMetricReq;
import com.tencent.supersonic.headless.api.response.MetricResp;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;

import java.util.List;
import java.util.Set;

public interface MetricService {

    void createMetric(MetricReq metricReq, User user) throws Exception;

    void createMetricBatch(List<MetricReq> metricReqs, User user) throws Exception;

    void updateExprMetric(MetricReq metricReq, User user) throws Exception;

    void batchUpdateStatus(MetaBatchReq metaBatchReq, User user);

    void deleteMetric(Long id, User user) throws Exception;

    PageInfo<MetricResp> queryMetric(PageMetricReq pageMetricReq, User user);

    List<MetricResp> getMetrics(MetaFilter metaFilter);

    MetricResp getMetric(Long modelId, String bizName);

    MetricResp getMetric(Long id, User user);

    MetricResp getMetric(Long id);

    List<String> mockAlias(MetricReq metricReq, String mockType, User user);

    Set<String> getMetricTags();

    List<DrillDownDimension> getDrillDownDimension(Long metricId);

    List<DataItem> getDataItems(Long modelId);

    void saveMetricQueryDefaultConfig(MetricQueryDefaultConfig defaultConfig, User user);

    MetricQueryDefaultConfig getMetricQueryDefaultConfig(Long metricId, User user);

    void sendMetricEventBatch(List<Long> modelIds, EventType eventType);
}
