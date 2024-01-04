package com.tencent.supersonic.chat.server.service;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.request.DictLatestTaskReq;
import com.tencent.supersonic.chat.api.pojo.request.DictTaskFilterReq;
import com.tencent.supersonic.chat.api.pojo.response.DictLatestTaskResp;
import com.tencent.supersonic.chat.core.knowledge.DictConfig;
import com.tencent.supersonic.chat.core.knowledge.DimValue2DictCommand;
import com.tencent.supersonic.chat.core.knowledge.DimValueDictInfo;
import java.util.List;

public interface KnowledgeTaskService {
    Long addDictTask(DimValue2DictCommand dimValue2DictCommend, User user);

    Long deleteDictTask(DimValue2DictCommand dimValue2DictCommend, User user);

    List<DimValueDictInfo> searchDictTaskList(DictTaskFilterReq filter, User user);

    DictConfig getDictInfoByModelId(Long modelId);

    String getDictRootPath();

    List<DictLatestTaskResp> searchDictLatestTaskList(DictLatestTaskReq filter, User user);
}
