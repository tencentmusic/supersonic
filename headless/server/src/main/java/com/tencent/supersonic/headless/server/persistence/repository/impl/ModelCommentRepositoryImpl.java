package com.tencent.supersonic.headless.server.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tencent.supersonic.headless.server.persistence.dataobject.ModelCommentDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ModelCommentDOMapper;
import com.tencent.supersonic.headless.server.persistence.repository.ModelCommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.util.List;


@Component
public class ModelCommentRepositoryImpl implements ModelCommentRepository {

    @Autowired
    private ModelCommentDOMapper modelCommentDOMapper;
    @Override
    public List<ModelCommentDO> getModelCommentList(Long modelId) {
        QueryWrapper<ModelCommentDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("model_id", modelId);
        queryWrapper.orderByDesc("created_at");
        return modelCommentDOMapper.selectList(queryWrapper);
    }

}
