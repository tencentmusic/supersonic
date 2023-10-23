package com.tencent.supersonic.semantic.model.infrastructure.repository;

import com.tencent.supersonic.common.pojo.enums.StatusEnum;
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
        modelDOMapper.updateByPrimaryKeySelective(modelDO);
    }

    @Override
    public void deleteModel(Long id) {
        ModelDO modelDO = modelDOMapper.selectByPrimaryKey(id);
        modelDO.setStatus(StatusEnum.DELETED.getCode());
        modelDOMapper.updateByPrimaryKey(modelDO);
    }

    @Override
    public List<ModelDO> getModelList() {
        ModelDOExample modelDOExample = new ModelDOExample();
        modelDOExample.createCriteria().andStatusNotEqualTo(StatusEnum.DELETED.getCode());
        return modelDOMapper.selectByExampleWithBLOBs(modelDOExample);
    }

    @Override
    public ModelDO getModelById(Long id) {
        return modelDOMapper.selectByPrimaryKey(id);
    }
}
