package com.tencent.supersonic.headless.server.persistence.repository;

import com.tencent.supersonic.headless.server.persistence.dataobject.MetricDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.MetricQueryDefaultConfigDO;
import com.tencent.supersonic.headless.server.pojo.MetricFilter;
import com.tencent.supersonic.headless.server.pojo.MetricsFilter;

import java.util.List;

public interface MetricRepository {

    Long createMetric(MetricDO metricDO);

    void createMetricBatch(List<MetricDO> metricDOS);

    void updateMetric(MetricDO metricDO);

    void batchUpdateStatus(List<MetricDO> metricDOS);

    void batchPublish(List<MetricDO> metricDOS);

    void batchUnPublish(List<MetricDO> metricDOS);

    void updateClassificationsBatch(List<MetricDO> metricDOS);

    MetricDO getMetricById(Long id);

    List<MetricDO> getMetric(MetricFilter metricFilter);

    List<MetricDO> getMetrics(MetricsFilter metricsFilter);

    void saveDefaultQueryConfig(MetricQueryDefaultConfigDO defaultConfigDO);

    void updateDefaultQueryConfig(MetricQueryDefaultConfigDO defaultConfigDO);

    MetricQueryDefaultConfigDO getDefaultQueryConfig(Long metricId, String userName);
}
