package com.tencent.supersonic.semantic.model.domain;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.semantic.api.model.request.MetricReq;
import com.tencent.supersonic.semantic.api.model.request.PageMetricReq;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;

import java.util.List;

public interface MetricService {

    List<MetricResp> getMetrics(List<Long> ids);

    List<MetricResp> getMetrics(Long modelId);

    List<MetricResp> getMetrics();

    List<MetricResp> getMetrics(Long modelId, Long datasourceId);

    void creatExprMetric(MetricReq metricReq, User user) throws Exception;

    void createMetricBatch(List<MetricReq> metricReqs, User user) throws Exception;

    PageInfo<MetricResp> queryMetric(PageMetricReq pageMetrricReq);

    MetricResp getMetric(Long modelId, String bizName);

    List<MetricResp> getHighSensitiveMetric(Long modelId);

    void updateExprMetric(MetricReq metricReq, User user) throws Exception;

    List<MetricResp> getAllHighSensitiveMetric();

    void deleteMetric(Long id) throws Exception;

    List<String> mockAlias(MetricReq metricReq, String mockType, User user);
}
