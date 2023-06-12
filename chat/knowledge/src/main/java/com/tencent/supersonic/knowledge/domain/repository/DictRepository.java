package com.tencent.supersonic.knowledge.domain.repository;

import com.tencent.supersonic.knowledge.domain.dataobject.DictConfPO;
import com.tencent.supersonic.knowledge.domain.dataobject.DimValueDictTaskPO;
import com.tencent.supersonic.knowledge.domain.pojo.DictConfig;
import com.tencent.supersonic.knowledge.domain.pojo.DictTaskFilter;
import com.tencent.supersonic.knowledge.domain.pojo.DimValueDictInfo;
import java.util.List;

public interface DictRepository {

    Long createDimValueDictTask(DimValueDictTaskPO dimValueDictTaskPO);

    Boolean updateDictTaskStatus(Integer status, DimValueDictTaskPO dimValueDictTaskPO);

    List<DimValueDictInfo> searchDictTaskList(DictTaskFilter filter);

    Boolean createDictConf(DictConfPO dictConfPO);

    Boolean editDictConf(DictConfPO dictConfPO);

    Boolean upsertDictInfo(DictConfPO dictConfPO);

    DictConfig getDictInfoByDomainId(Long domainId);
}
