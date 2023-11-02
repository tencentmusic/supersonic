package com.tencent.supersonic.semantic.model.domain.repository;

import com.tencent.supersonic.semantic.model.domain.dataobject.DimensionDO;
import com.tencent.supersonic.semantic.model.domain.pojo.DimensionFilter;
import java.util.List;

public interface DimensionRepository {


    void createDimension(DimensionDO dimensionDO);

    void createDimensionBatch(List<DimensionDO> dimensionDOS);

    void updateDimension(DimensionDO dimensionDO);

    void batchUpdateStatus(List<DimensionDO> dimensionDOS);

    DimensionDO getDimensionById(Long id);

    List<DimensionDO> getDimension(DimensionFilter dimensionFilter);
}
