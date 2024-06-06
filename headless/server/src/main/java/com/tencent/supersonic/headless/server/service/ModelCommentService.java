package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.headless.server.persistence.dataobject.ModelCommentDO;
import java.util.List;

public interface ModelCommentService {

    List<ModelCommentDO> getModelCommentList(Long modelId);


}
