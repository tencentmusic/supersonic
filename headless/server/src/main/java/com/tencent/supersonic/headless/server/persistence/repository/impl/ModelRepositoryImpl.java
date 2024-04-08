package com.tencent.supersonic.headless.server.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.headless.server.persistence.dataobject.ModelDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ModelDOCustomMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ModelDOMapper;
import com.tencent.supersonic.headless.server.persistence.repository.ModelRepository;
import com.tencent.supersonic.headless.server.pojo.ModelFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;


@Component
public class ModelRepositoryImpl implements ModelRepository {


    private ModelDOMapper modelDOMapper;

    private ModelDOCustomMapper modelDOCustomMapper;

    public ModelRepositoryImpl(ModelDOMapper modelDOMapper,
                               ModelDOCustomMapper modelDOCustomMapper) {
        this.modelDOMapper = modelDOMapper;
        this.modelDOCustomMapper = modelDOCustomMapper;
    }

    @Override
    public void createModel(ModelDO datasourceDO) {
        modelDOMapper.insert(datasourceDO);
    }

    @Override
    public void updateModel(ModelDO datasourceDO) {
        modelDOMapper.updateById(datasourceDO);
    }

    @Override
    public List<ModelDO> getModelList(ModelFilter modelFilter) {
        QueryWrapper<ModelDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().ne(ModelDO::getStatus, StatusEnum.DELETED.getCode());
        if (modelFilter.getDatabaseId() != null) {
            wrapper.lambda().eq(ModelDO::getDatabaseId, modelFilter.getDatabaseId());
        }
        if (!CollectionUtils.isEmpty(modelFilter.getDomainIds())) {
            wrapper.lambda().in(ModelDO::getDomainId, modelFilter.getDomainIds());
        }
        if (modelFilter.getDomainId() != null) {
            wrapper.lambda().eq(ModelDO::getDomainId, modelFilter.getDomainId());
        }
        if (!CollectionUtils.isEmpty(modelFilter.getIds())) {
            wrapper.lambda().in(ModelDO::getId, modelFilter.getIds());
        }
        if (!CollectionUtils.isEmpty(modelFilter.getModelIds())) {
            wrapper.lambda().in(ModelDO::getId, modelFilter.getModelIds());
        }
        if (modelFilter.getIncludesDetail() != null && !modelFilter.getIncludesDetail()) {
            wrapper.select(ModelDO.class, modelDO -> !modelDO.getColumn().equals("model_detail"));
        }
        return modelDOMapper.selectList(wrapper);
    }

    @Override
    public ModelDO getModelById(Long id) {
        return modelDOMapper.selectById(id);
    }

    @Override
    public void batchUpdate(List<ModelDO> modelDOS) {
        modelDOCustomMapper.batchUpdateStatus(modelDOS);
    }

}
