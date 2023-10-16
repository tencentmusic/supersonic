package com.tencent.supersonic.semantic.model.domain.repository;

import com.tencent.supersonic.semantic.model.domain.dataobject.DimensionDO;
import com.tencent.supersonic.semantic.model.domain.pojo.DimensionFilter;
import java.util.List;

public interface DimensionRepository {


    void createDimension(DimensionDO dimensionDO);

    void createDimensionBatch(List<DimensionDO> dimensionDOS);

    void updateDimension(DimensionDO dimensionDO);

    List<DimensionDO> getDimensionListOfDatasource(Long datasourceId);

    List<DimensionDO> getDimensionListOfmodel(Long domainId);

    List<DimensionDO> getDimensionListOfmodelIds(List<Long> modelIds);

    List<DimensionDO> getDimensionList();

    List<DimensionDO> getDimensionListByIds(List<Long> ids);

    DimensionDO getDimensionById(Long id);


    List<DimensionDO> getAllDimensionList();

    List<DimensionDO> getDimension(DimensionFilter dimensionFilter);

    void deleteDimension(Long id);
}
