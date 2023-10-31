package com.tencent.supersonic.semantic.model.domain.repository;


import com.tencent.supersonic.semantic.model.domain.pojo.MetricFilter;
import com.tencent.supersonic.semantic.model.domain.dataobject.MetricDO;

import java.util.List;


public interface MetricRepository {

    Long createMetric(MetricDO metricDO);

    void createMetricBatch(List<MetricDO> metricDOS);

    void updateMetric(MetricDO metricDO);

    List<MetricDO> getMetricList(Long domainId);

    List<MetricDO> getMetricList(List<Long> modelIds);

    List<MetricDO> getMetricList();

    List<MetricDO> getMetricListByIds(List<Long> ids);

    MetricDO getMetricById(Long id);

    List<MetricDO> getAllMetricList();

    List<MetricDO> getMetric(MetricFilter metricFilter);

    void deleteMetric(Long id);
}
