package com.tencent.supersonic.headless.server.persistence.repository;

import com.tencent.supersonic.headless.server.persistence.dataobject.DimensionDO;
import com.tencent.supersonic.headless.server.pojo.DimensionFilter;
import com.tencent.supersonic.headless.server.pojo.DimensionsFilter;

import java.util.List;

public interface DimensionRepository {

    void createDimension(DimensionDO dimensionDO);

    void createDimensionBatch(List<DimensionDO> dimensionDOS);

    void updateDimension(DimensionDO dimensionDO);

    void batchUpdateStatus(List<DimensionDO> dimensionDOS);

    DimensionDO getDimensionById(Long id);

    List<DimensionDO> getDimension(DimensionFilter dimensionFilter);

    List<DimensionDO> getDimensions(DimensionsFilter dimensionsFilter);
}
