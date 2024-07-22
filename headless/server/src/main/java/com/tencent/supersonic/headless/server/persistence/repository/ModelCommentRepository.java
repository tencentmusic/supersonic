package com.tencent.supersonic.headless.server.persistence.repository;

import com.tencent.supersonic.headless.server.persistence.dataobject.ModelCommentDO;
import java.util.List;

public interface ModelCommentRepository {

    List<ModelCommentDO> getModelCommentList(Long modelId);

}
