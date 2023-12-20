package com.tencent.supersonic.headless.model.infrastructure.mapper;

import com.tencent.supersonic.headless.model.domain.pojo.MetricFilter;
import com.tencent.supersonic.headless.model.domain.dataobject.MetricDO;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MetricDOCustomMapper {

    void batchInsert(List<MetricDO> metricDOS);

    void batchUpdate(List<MetricDO> metricDOS);

    void batchUpdateStatus(List<MetricDO> metricDOS);

    List<MetricDO> query(MetricFilter metricFilter);

}
