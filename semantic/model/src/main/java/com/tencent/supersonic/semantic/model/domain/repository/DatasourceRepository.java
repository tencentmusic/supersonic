package com.tencent.supersonic.semantic.model.domain.repository;


import com.tencent.supersonic.semantic.model.domain.dataobject.DatasourceDO;
import com.tencent.supersonic.semantic.model.domain.dataobject.DatasourceRelaDO;
import java.util.List;

public interface DatasourceRepository {

    void createDatasource(DatasourceDO datasourceDO);

    void updateDatasource(DatasourceDO datasourceDO);

    List<DatasourceDO> getDatasourceList();

    List<DatasourceDO> getDatasourceList(Long modelId);

    List<DatasourceDO> getDatasourceByDatabase(Long databaseId);

    DatasourceDO getDatasourceById(Long id);

    void createDatasourceRela(DatasourceRelaDO datasourceRelaDO);

    void updateDatasourceRela(DatasourceRelaDO datasourceRelaDO);

    DatasourceRelaDO getDatasourceRelaById(Long id);

    List<DatasourceRelaDO> getDatasourceRelaList(Long modelId);

    void deleteDatasourceRela(Long id);
}
