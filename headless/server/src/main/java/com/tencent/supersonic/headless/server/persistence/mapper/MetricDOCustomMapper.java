package com.tencent.supersonic.headless.server.persistence.mapper;

import com.tencent.supersonic.headless.server.persistence.dataobject.MetricDO;
import com.tencent.supersonic.headless.server.pojo.MetricsFilter;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MetricDOCustomMapper {

    void batchInsert(List<MetricDO> metricDOS);

    void batchUpdateStatus(List<MetricDO> metricDOS);

    void batchPublish(List<MetricDO> metricDOS);

    void batchUnPublish(List<MetricDO> metricDOS);

    void updateClassificationsBatch(List<MetricDO> metricDOS);

    List<MetricDO> queryMetrics(MetricsFilter metricsFilter);
}
