package com.tencent.supersonic.headless.server.persistence.repository;

import com.tencent.supersonic.headless.api.pojo.request.DictItemFilter;
import com.tencent.supersonic.headless.api.pojo.request.DictSingleTaskReq;
import com.tencent.supersonic.headless.api.pojo.request.ValueTaskQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.DictItemResp;
import com.tencent.supersonic.headless.api.pojo.response.DictTaskResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.DictConfDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.DictTaskDO;

import java.util.List;

public interface DictRepository {

    Long addDictConf(DictConfDO dictConfDO);

    Long editDictConf(DictConfDO dictConfDO);

    List<DictItemResp> queryDictConf(DictItemFilter dictItemFilter);

    Long addDictTask(DictTaskDO dictTaskDO);

    Long editDictTask(DictTaskDO dictTaskDO);

    DictTaskDO queryDictTask(DictItemFilter filter);

    DictTaskDO queryDictTaskById(Long id);

    DictTaskResp queryLatestDictTask(DictSingleTaskReq taskReq);

    List<DictTaskDO> queryAllDictTask(ValueTaskQueryReq taskQueryReq);
}
