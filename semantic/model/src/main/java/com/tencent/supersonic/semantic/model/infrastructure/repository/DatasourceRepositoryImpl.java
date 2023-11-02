package com.tencent.supersonic.semantic.model.infrastructure.repository;

import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.semantic.model.domain.dataobject.DatasourceDO;
import com.tencent.supersonic.semantic.model.domain.dataobject.DatasourceDOExample;
import com.tencent.supersonic.semantic.model.domain.dataobject.DatasourceRelaDO;
import com.tencent.supersonic.semantic.model.domain.dataobject.DatasourceRelaDOExample;
import com.tencent.supersonic.semantic.model.domain.repository.DatasourceRepository;
import com.tencent.supersonic.semantic.model.infrastructure.mapper.DatasourceDOMapper;
import com.tencent.supersonic.semantic.model.infrastructure.mapper.DatasourceRelaDOMapper;
import java.util.List;
import org.springframework.stereotype.Component;


@Component
public class DatasourceRepositoryImpl implements DatasourceRepository {


    private DatasourceDOMapper datasourceMapper;

    private DatasourceRelaDOMapper datasourceRelaDOMapper;

    public DatasourceRepositoryImpl(DatasourceDOMapper datasourceMapper,
                                    DatasourceRelaDOMapper datasourceRelaDOMapper) {
        this.datasourceMapper = datasourceMapper;
        this.datasourceRelaDOMapper = datasourceRelaDOMapper;
    }


    @Override
    public void createDatasource(DatasourceDO datasourceDO) {
        datasourceMapper.insert(datasourceDO);
    }

    @Override
    public void updateDatasource(DatasourceDO datasourceDO) {
        datasourceMapper.updateByPrimaryKeySelective(datasourceDO);
    }

    @Override
    public List<DatasourceDO> getDatasourceList() {
        DatasourceDOExample datasourceExample = new DatasourceDOExample();
        datasourceExample.createCriteria().andStatusNotEqualTo(StatusEnum.DELETED.getCode());
        return datasourceMapper.selectByExampleWithBLOBs(datasourceExample);
    }

    @Override
    public List<DatasourceDO> getDatasourceList(Long modelId) {
        DatasourceDOExample datasourceExample = new DatasourceDOExample();
        datasourceExample.createCriteria().andModelIdEqualTo(modelId)
                .andStatusNotEqualTo(StatusEnum.DELETED.getCode());
        return datasourceMapper.selectByExampleWithBLOBs(datasourceExample);
    }

    @Override
    public List<DatasourceDO> getDatasourceByDatabase(Long databaseId) {
        DatasourceDOExample datasourceExample = new DatasourceDOExample();
        datasourceExample.createCriteria().andDatabaseIdEqualTo(databaseId)
                .andStatusNotEqualTo(StatusEnum.DELETED.getCode());
        return datasourceMapper.selectByExampleWithBLOBs(datasourceExample);
    }

    @Override
    public DatasourceDO getDatasourceById(Long id) {
        return datasourceMapper.selectByPrimaryKey(id);
    }

    @Override
    public void createDatasourceRela(DatasourceRelaDO datasourceRelaDO) {
        datasourceRelaDOMapper.insert(datasourceRelaDO);
    }

    @Override
    public void updateDatasourceRela(DatasourceRelaDO datasourceRelaDO) {
        datasourceRelaDOMapper.updateByPrimaryKey(datasourceRelaDO);
    }

    @Override
    public DatasourceRelaDO getDatasourceRelaById(Long id) {
        return datasourceRelaDOMapper.selectByPrimaryKey(id);
    }


    @Override
    public List<DatasourceRelaDO> getDatasourceRelaList(Long modelId) {
        DatasourceRelaDOExample datasourceRelaDOExample = new DatasourceRelaDOExample();
        datasourceRelaDOExample.createCriteria().andModelIdEqualTo(modelId);
        return datasourceRelaDOMapper.selectByExample(datasourceRelaDOExample);
    }

    @Override
    public void deleteDatasourceRela(Long id) {
        datasourceRelaDOMapper.deleteByPrimaryKey(id);
    }

}
