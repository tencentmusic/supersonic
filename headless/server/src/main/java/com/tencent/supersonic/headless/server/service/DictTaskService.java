package com.tencent.supersonic.headless.server.service;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.DictSingleTaskReq;
import com.tencent.supersonic.headless.api.pojo.request.DictValueReq;
import com.tencent.supersonic.headless.api.pojo.request.ValueTaskQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.DictTaskResp;
import com.tencent.supersonic.headless.api.pojo.response.DictValueDimResp;

/** Manage dictionary tasks */
public interface DictTaskService {
    Long addDictTask(DictSingleTaskReq taskReq, User user);

    Long deleteDictTask(DictSingleTaskReq taskReq, User user);

    Boolean dailyDictTask();

    DictTaskResp queryLatestDictTask(DictSingleTaskReq taskReq, User user);

    PageInfo<DictTaskResp> queryDictTask(ValueTaskQueryReq taskQueryReq, User user);

    PageInfo<DictValueDimResp> queryDictValue(DictValueReq dictValueReq, User user);

    String queryDictFilePath(DictValueReq dictValueReq, User user);
}
