package com.tencent.supersonic.headless.server.service.impl;
import com.tencent.supersonic.headless.server.persistence.dataobject.ModelCommentDO;
import com.tencent.supersonic.headless.server.persistence.repository.ModelCommentRepository;
import com.tencent.supersonic.headless.server.service.ModelCommentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ModelCommentServiceImpl implements ModelCommentService {

    @Autowired
    private ModelCommentRepository modelCommentRepository;

    @Override
    public List<ModelCommentDO> getModelCommentList(Long modelId) {
        return modelCommentRepository.getModelCommentList(modelId);
    }

}
