package com.tencent.supersonic.semantic.model.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.semantic.model.domain.dataobject.DatasourceDO;
import com.tencent.supersonic.semantic.model.domain.repository.DatasourceRepository;
import com.tencent.supersonic.semantic.model.infrastructure.mapper.DatasourceDOMapper;

import java.util.List;
import org.springframework.stereotype.Component;


@Component
public class DatasourceRepositoryImpl implements DatasourceRepository {


    private DatasourceDOMapper datasourceMapper;

    public DatasourceRepositoryImpl(DatasourceDOMapper datasourceMapper) {
        this.datasourceMapper = datasourceMapper;
    }


    @Override
    public void createDatasource(DatasourceDO datasourceDO) {
        datasourceMapper.insert(datasourceDO);
    }

    @Override
    public void updateDatasource(DatasourceDO datasourceDO) {
        datasourceMapper.updateById(datasourceDO);
    }

    @Override
    public List<DatasourceDO> getDatasourceList() {
        QueryWrapper<DatasourceDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().ne(DatasourceDO::getStatus, StatusEnum.DELETED.getCode());
        return datasourceMapper.selectList(wrapper);
    }

    @Override
    public List<DatasourceDO> getDatasourceList(Long modelId) {
        QueryWrapper<DatasourceDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().ne(DatasourceDO::getStatus, StatusEnum.DELETED.getCode())
                .eq(DatasourceDO::getModelId, modelId);
        return datasourceMapper.selectList(wrapper);
    }

    @Override
    public List<DatasourceDO> getDatasourceByDatabase(Long databaseId) {
        QueryWrapper<DatasourceDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().ne(DatasourceDO::getStatus, StatusEnum.DELETED.getCode())
                .eq(DatasourceDO::getDatabaseId, databaseId);
        return datasourceMapper.selectList(wrapper);
    }

    @Override
    public DatasourceDO getDatasourceById(Long id) {
        return datasourceMapper.selectById(id);
    }

}
