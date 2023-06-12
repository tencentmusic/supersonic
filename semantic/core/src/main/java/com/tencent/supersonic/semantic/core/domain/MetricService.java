package com.tencent.supersonic.semantic.core.domain;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.semantic.api.core.request.MetricReq;
import com.tencent.supersonic.semantic.api.core.request.PageMetricReq;
import com.tencent.supersonic.semantic.api.core.response.MetricResp;
import java.util.List;

public interface MetricService {

    List<MetricResp> getMetrics(List<Long> ids);

    List<MetricResp> getMetrics(Long domainId);

    List<MetricResp> getMetrics(Long domainId, Long datasourceId);

    void creatExprMetric(MetricReq metricReq, User user) throws Exception;

    void createMetricBatch(List<MetricReq> metricReqs, User user) throws Exception;

    PageInfo<MetricResp> queryMetric(PageMetricReq pageMetrricReq);

    MetricResp getMetric(Long domainId, String bizName);

    List<MetricResp> getHighSensitiveMetric(Long domainId);

    void updateExprMetric(MetricReq metricReq, User user) throws Exception;

    List<MetricResp> getAllHighSensitiveMetric();

    void deleteMetric(Long id) throws Exception;
}
