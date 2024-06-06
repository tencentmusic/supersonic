package com.tencent.supersonic.headless.server.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.headless.server.persistence.dataobject.ModelCommentDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ModelDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ModelCommentDOMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ModelDOCustomMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ModelDOMapper;
import com.tencent.supersonic.headless.server.persistence.repository.ModelCommentRepository;
import com.tencent.supersonic.headless.server.pojo.ModelFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;


@Component
public class ModelCommentRepositoryImpl implements ModelCommentRepository {
    private ModelCommentDOMapper modelCommentDOMapper;
    @Override
    public List<ModelCommentDO> getModelCommentList(Long modelId) {
        QueryWrapper<ModelCommentDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("model_id", modelId);
        queryWrapper.orderByDesc("create_time");
        return modelCommentDOMapper.selectList(queryWrapper);
    }

}
