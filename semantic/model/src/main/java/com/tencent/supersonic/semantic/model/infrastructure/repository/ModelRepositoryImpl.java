package com.tencent.supersonic.semantic.model.infrastructure.repository;

import com.tencent.supersonic.semantic.model.domain.dataobject.ModelDO;
import com.tencent.supersonic.semantic.model.domain.dataobject.ModelDOExample;
import com.tencent.supersonic.semantic.model.domain.repository.ModelRepository;
import com.tencent.supersonic.semantic.model.infrastructure.mapper.ModelDOMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ModelRepositoryImpl implements ModelRepository {

    private ModelDOMapper modelDOMapper;

    public ModelRepositoryImpl(ModelDOMapper modelDOMapper) {
        this.modelDOMapper = modelDOMapper;
    }

    @Override
    public void createModel(ModelDO modelDO) {
        modelDOMapper.insert(modelDO);
    }

    @Override
    public void updateModel(ModelDO modelDO) {
        modelDOMapper.updateByPrimaryKeyWithBLOBs(modelDO);
    }

    @Override
    public void deleteModel(Long id) {
        modelDOMapper.deleteByPrimaryKey(id);
    }

    @Override
    public List<ModelDO> getModelList() {
        return modelDOMapper.selectByExampleWithBLOBs(new ModelDOExample());
    }

    @Override
    public ModelDO getModelById(Long id) {
        return modelDOMapper.selectByPrimaryKey(id);
    }
}
