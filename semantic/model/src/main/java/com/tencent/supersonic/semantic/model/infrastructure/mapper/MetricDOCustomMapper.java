package com.tencent.supersonic.semantic.model.infrastructure.mapper;

import com.tencent.supersonic.semantic.model.domain.dataobject.MetricDO;
import com.tencent.supersonic.semantic.model.domain.pojo.MetricFilter;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MetricDOCustomMapper {

    void batchInsert(List<MetricDO> metricDOS);

    void batchUpdate(List<MetricDO> metricDOS);

    List<MetricDO> query(MetricFilter metricFilter);

}
