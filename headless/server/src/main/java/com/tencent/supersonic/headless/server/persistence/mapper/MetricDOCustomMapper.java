package com.tencent.supersonic.headless.server.persistence.mapper;

import com.tencent.supersonic.headless.server.persistence.dataobject.MetricDO;
import com.tencent.supersonic.headless.server.pojo.MetricFilter;
import com.tencent.supersonic.headless.server.pojo.MetricsFilter;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MetricDOCustomMapper {

    void batchInsert(List<MetricDO> metricDOS);

    void batchUpdateStatus(List<MetricDO> metricDOS);

    List<MetricDO> query(MetricFilter metricFilter);

    List<MetricDO> queryMetrics(MetricsFilter metricsFilter);

}
