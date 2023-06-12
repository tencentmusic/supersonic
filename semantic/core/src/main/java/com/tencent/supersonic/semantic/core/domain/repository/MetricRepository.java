package com.tencent.supersonic.semantic.core.domain.repository;


import com.tencent.supersonic.semantic.core.domain.pojo.MetricFilter;
import com.tencent.supersonic.semantic.core.domain.dataobject.MetricDO;

import java.util.List;


public interface MetricRepository {

    Long createMetric(MetricDO metricDO);

    void createMetricBatch(List<MetricDO> metricDOS);

    void updateMetric(MetricDO metricDO);

    List<MetricDO> getMetricList(Long classId);

    List<MetricDO> getMetricListByIds(List<Long> ids);

    MetricDO getMetricById(Long id);

    List<MetricDO> getAllMetricList();

    List<MetricDO> getMetric(MetricFilter metricFilter);

    void deleteMetric(Long id);
}
