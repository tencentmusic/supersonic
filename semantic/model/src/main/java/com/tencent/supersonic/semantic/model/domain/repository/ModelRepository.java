package com.tencent.supersonic.semantic.model.domain.repository;

import com.tencent.supersonic.semantic.model.domain.dataobject.ModelDO;
import java.util.List;

public interface ModelRepository {

    void createModel(ModelDO modelDO);

    void updateModel(ModelDO modelDO);

    void deleteModel(Long id);

    List<ModelDO> getModelList();

    ModelDO getModelById(Long id);

}
