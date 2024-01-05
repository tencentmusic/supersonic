package com.tencent.supersonic.chat.server.persistence.repository;


import com.tencent.supersonic.chat.api.pojo.request.DictTaskFilterReq;
import com.tencent.supersonic.chat.core.knowledge.DictConfig;
import com.tencent.supersonic.chat.core.knowledge.DimValueDictInfo;
import com.tencent.supersonic.chat.server.persistence.dataobject.DictTaskDO;
import java.util.List;

public interface DictRepository {

    Long createDimValueDictTask(DictTaskDO dictTaskDO);

    Boolean updateDictTaskStatus(Integer status, DictTaskDO dictTaskDO);

    List<DimValueDictInfo> searchDictTaskList(DictTaskFilterReq filter);

    DictConfig getDictInfoByModelId(Long modelId);
}
