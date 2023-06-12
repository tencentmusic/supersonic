package com.tencent.supersonic.semantic.core.domain.repository;

import com.tencent.supersonic.semantic.core.domain.dataobject.DimensionDO;
import com.tencent.supersonic.semantic.core.domain.pojo.DimensionFilter;
import java.util.List;


public interface DimensionRepository {


    void createDimension(DimensionDO dimensionDO);

    void createDimensionBatch(List<DimensionDO> dimensionDOS);

    void updateDimension(DimensionDO dimensionDO);

    List<DimensionDO> getDimensionListOfDatasource(Long datasourceId);

    List<DimensionDO> getDimensionListOfDomain(Long domainId);

    List<DimensionDO> getDimensionListByIds(List<Long> ids);

    DimensionDO getDimensionById(Long id);


    List<DimensionDO> getAllDimensionList();

    List<DimensionDO> getDimension(DimensionFilter dimensionFilter);

    void deleteDimension(Long id);
}
