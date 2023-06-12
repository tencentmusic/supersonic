package com.tencent.supersonic.semantic.core.domain.repository;


import com.tencent.supersonic.semantic.core.domain.dataobject.DatasourceDO;
import com.tencent.supersonic.semantic.core.domain.dataobject.DatasourceRelaDO;

import java.util.List;


public interface DatasourceRepository {

    void createDatasource(DatasourceDO datasourceDO);

    void updateDatasource(DatasourceDO datasourceDO);

    List<DatasourceDO> getDatasourceList();

    List<DatasourceDO> getDatasourceList(Long classId);

    DatasourceDO getDatasourceById(Long id);

    void deleteDatasource(Long id);

    void createDatasourceRela(DatasourceRelaDO datasourceRelaDO);

    void updateDatasourceRela(DatasourceRelaDO datasourceRelaDO);

    DatasourceRelaDO getDatasourceRelaById(Long id);

    List<DatasourceRelaDO> getDatasourceRelaList(Long domainId);

    void deleteDatasourceRela(Long id);
}
