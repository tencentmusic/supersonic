package com.tencent.supersonic.semantic.core.infrastructure.mapper;


import com.tencent.supersonic.semantic.core.domain.dataobject.MetricDO;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface MetricDOCustomMapper {

    void batchInsert(List<MetricDO> metricDOS);

    void batchUpdate(List<MetricDO> metricDOS);

}
