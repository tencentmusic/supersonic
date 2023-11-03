package com.tencent.supersonic.semantic.model.domain.repository;


import com.tencent.supersonic.semantic.model.domain.pojo.MetricFilter;
import com.tencent.supersonic.semantic.model.domain.dataobject.MetricDO;
import java.util.List;


public interface MetricRepository {

    Long createMetric(MetricDO metricDO);

    void createMetricBatch(List<MetricDO> metricDOS);

    void updateMetric(MetricDO metricDO);

    void batchUpdateStatus(List<MetricDO> metricDOS);

    MetricDO getMetricById(Long id);

    List<MetricDO> getMetric(MetricFilter metricFilter);
}
