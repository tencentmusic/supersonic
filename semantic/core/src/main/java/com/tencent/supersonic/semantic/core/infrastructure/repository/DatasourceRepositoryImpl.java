package com.tencent.supersonic.semantic.core.infrastructure.repository;

import com.tencent.supersonic.semantic.core.domain.dataobject.DatasourceDO;
import com.tencent.supersonic.semantic.core.domain.dataobject.DatasourceDOExample;
import com.tencent.supersonic.semantic.core.domain.dataobject.DatasourceRelaDO;
import com.tencent.supersonic.semantic.core.domain.dataobject.DatasourceRelaDOExample;
import com.tencent.supersonic.semantic.core.domain.repository.DatasourceRepository;
import com.tencent.supersonic.semantic.core.infrastructure.mapper.DatasourceDOMapper;
import com.tencent.supersonic.semantic.core.infrastructure.mapper.DatasourceRelaDOMapper;
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
        datasourceMapper.updateByPrimaryKeyWithBLOBs(datasourceDO);
    }

    @Override
    public List<DatasourceDO> getDatasourceList() {
        DatasourceDOExample datasourceExample = new DatasourceDOExample();
        return datasourceMapper.selectByExampleWithBLOBs(datasourceExample);
    }

    @Override
    public List<DatasourceDO> getDatasourceList(Long classId) {
        DatasourceDOExample datasourceExample = new DatasourceDOExample();
        datasourceExample.createCriteria().andDomainIdEqualTo(classId);
        return datasourceMapper.selectByExampleWithBLOBs(datasourceExample);
    }

    @Override
    public DatasourceDO getDatasourceById(Long id) {
        return datasourceMapper.selectByPrimaryKey(id);
    }
    
    @Override
    public void deleteDatasource(Long id) {
        datasourceMapper.deleteByPrimaryKey(id);
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
    public List<DatasourceRelaDO> getDatasourceRelaList(Long domainId) {
        DatasourceRelaDOExample datasourceRelaDOExample = new DatasourceRelaDOExample();
        datasourceRelaDOExample.createCriteria().andDomainIdEqualTo(domainId);
        return datasourceRelaDOMapper.selectByExample(datasourceRelaDOExample);
    }

    @Override
    public void deleteDatasourceRela(Long id) {
        datasourceRelaDOMapper.deleteByPrimaryKey(id);
    }

}
