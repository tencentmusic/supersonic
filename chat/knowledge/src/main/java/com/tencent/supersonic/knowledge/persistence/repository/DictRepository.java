package com.tencent.supersonic.knowledge.persistence.repository;


import com.tencent.supersonic.knowledge.persistence.dataobject.DictTaskDO;
import com.tencent.supersonic.knowledge.dictionary.DictConfig;
import com.tencent.supersonic.knowledge.dictionary.DictTaskFilter;
import com.tencent.supersonic.knowledge.dictionary.DimValueDictInfo;

import java.util.List;

public interface DictRepository {

    Long createDimValueDictTask(DictTaskDO dictTaskDO);

    Boolean updateDictTaskStatus(Integer status, DictTaskDO dictTaskDO);

    List<DimValueDictInfo> searchDictTaskList(DictTaskFilter filter);

    DictConfig getDictInfoByModelId(Long modelId);
}
