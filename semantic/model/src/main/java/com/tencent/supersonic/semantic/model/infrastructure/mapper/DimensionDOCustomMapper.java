package com.tencent.supersonic.semantic.model.infrastructure.mapper;


import com.tencent.supersonic.semantic.model.domain.dataobject.DimensionDO;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DimensionDOCustomMapper {

    void batchInsert(List<DimensionDO> dimensionDOS);

    void batchUpdate(List<DimensionDO> dimensionDOS);

}
