package com.tencent.supersonic.semantic.model.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.supersonic.semantic.model.domain.dataobject.MetricDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MetricDOMapper extends BaseMapper<MetricDO> {

}