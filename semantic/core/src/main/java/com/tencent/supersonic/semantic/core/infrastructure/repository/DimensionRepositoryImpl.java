package com.tencent.supersonic.semantic.core.infrastructure.repository;

import com.tencent.supersonic.semantic.core.domain.dataobject.DimensionDO;
import com.tencent.supersonic.semantic.core.domain.dataobject.DimensionDOExample;
import com.tencent.supersonic.semantic.core.domain.repository.DimensionRepository;
import com.tencent.supersonic.semantic.core.domain.pojo.DimensionFilter;
import com.tencent.supersonic.semantic.core.infrastructure.mapper.DimensionDOCustomMapper;
import com.tencent.supersonic.semantic.core.infrastructure.mapper.DimensionDOMapper;
import java.util.List;
import org.springframework.stereotype.Service;


@Service
public class DimensionRepositoryImpl implements DimensionRepository {

    private DimensionDOMapper dimensionDOMapper;

    private DimensionDOCustomMapper dimensionDOCustomMapper;


    public DimensionRepositoryImpl(DimensionDOMapper dimensionDOMapper,
            DimensionDOCustomMapper dimensionDOCustomMapper) {
        this.dimensionDOMapper = dimensionDOMapper;
        this.dimensionDOCustomMapper = dimensionDOCustomMapper;
    }


    @Override
    public void createDimension(DimensionDO dimensionDO) {

        dimensionDOMapper.insert(dimensionDO);

    }

    @Override
    public void createDimensionBatch(List<DimensionDO> dimensionDOS) {
        dimensionDOCustomMapper.batchInsert(dimensionDOS);
    }

    @Override
    public void updateDimension(DimensionDO dimensionDO) {
        dimensionDOMapper.updateByPrimaryKeyWithBLOBs(dimensionDO);
    }

    @Override
    public List<DimensionDO> getDimensionListOfDatasource(Long datasourceId) {
        DimensionDOExample dimensionDOExample = new DimensionDOExample();
        dimensionDOExample.createCriteria().andDatasourceIdEqualTo(datasourceId);
        return dimensionDOMapper.selectByExampleWithBLOBs(dimensionDOExample);
    }

    @Override
    public List<DimensionDO> getDimensionListOfDomain(Long domainId) {
        DimensionDOExample dimensionDOExample = new DimensionDOExample();
        dimensionDOExample.createCriteria().andDomainIdEqualTo(domainId);
        return dimensionDOMapper.selectByExampleWithBLOBs(dimensionDOExample);
    }

    @Override
    public List<DimensionDO> getDimensionListByIds(List<Long> ids) {
        DimensionDOExample dimensionDOExample = new DimensionDOExample();
        dimensionDOExample.createCriteria().andIdIn(ids);
        return dimensionDOMapper.selectByExampleWithBLOBs(dimensionDOExample);
    }

    @Override
    public DimensionDO getDimensionById(Long id) {
        return dimensionDOMapper.selectByPrimaryKey(id);
    }

    @Override
    public List<DimensionDO> getAllDimensionList() {
        DimensionDOExample dimensionDOExample = new DimensionDOExample();
        return dimensionDOMapper.selectByExampleWithBLOBs(dimensionDOExample);
    }


    @Override
    public List<DimensionDO> getDimension(DimensionFilter dimensionFilter) {
        DimensionDOExample dimensionDOExample = new DimensionDOExample();
        dimensionDOExample.createCriteria();
        if (dimensionFilter.getId() != null) {
            dimensionDOExample.getOredCriteria().get(0).andIdEqualTo(dimensionFilter.getId());
        }
        if (dimensionFilter.getName() != null) {
            dimensionDOExample.getOredCriteria().get(0).andNameLike("%" + dimensionFilter.getName() + "%");
        }
        if (dimensionFilter.getBizName() != null) {
            dimensionDOExample.getOredCriteria().get(0).andBizNameEqualTo("%" + dimensionFilter.getBizName() + "%");
        }
        if (dimensionFilter.getCreatedBy() != null) {
            dimensionDOExample.getOredCriteria().get(0).andCreatedByEqualTo(dimensionFilter.getCreatedBy());
        }
        if (dimensionFilter.getDomainId() != null) {
            dimensionDOExample.getOredCriteria().get(0).andDomainIdEqualTo(dimensionFilter.getDomainId());
        }
        return dimensionDOMapper.selectByExampleWithBLOBs(dimensionDOExample);
    }


    @Override
    public void deleteDimension(Long id) {
        dimensionDOMapper.deleteByPrimaryKey(id);
    }

}
