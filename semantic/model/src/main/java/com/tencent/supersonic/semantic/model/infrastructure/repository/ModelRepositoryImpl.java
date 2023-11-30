package com.tencent.supersonic.semantic.model.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.semantic.model.domain.dataobject.ModelDO;
import com.tencent.supersonic.semantic.model.domain.pojo.ModelFilter;
import com.tencent.supersonic.semantic.model.domain.repository.ModelRepository;
import com.tencent.supersonic.semantic.model.infrastructure.mapper.ModelDOCustomMapper;
import com.tencent.supersonic.semantic.model.infrastructure.mapper.ModelDOMapper;
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
        if (!CollectionUtils.isEmpty(modelFilter.getIds())) {
            wrapper.lambda().in(ModelDO::getId, modelFilter.getIds());
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
