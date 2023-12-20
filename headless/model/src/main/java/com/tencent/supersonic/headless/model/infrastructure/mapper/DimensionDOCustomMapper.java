package com.tencent.supersonic.headless.model.infrastructure.mapper;


import com.tencent.supersonic.headless.model.domain.pojo.DimensionFilter;
import com.tencent.supersonic.headless.model.domain.dataobject.DimensionDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DimensionDOCustomMapper {

    void batchInsert(List<DimensionDO> dimensionDOS);

    void batchUpdate(List<DimensionDO> dimensionDOS);

    void batchUpdateStatus(List<DimensionDO> dimensionDOS);

    List<DimensionDO> query(DimensionFilter dimensionFilter);
}
