package com.tencent.supersonic.headless.server.web.service;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.DictItemFilter;
import com.tencent.supersonic.headless.api.pojo.request.DictItemReq;
import com.tencent.supersonic.headless.api.pojo.response.DictItemResp;

import java.util.List;

/**
 * Make relevant settings for the dictionary
 */
public interface DictConfService {

    DictItemResp addDictConf(DictItemReq itemValueReq, User user);

    DictItemResp editDictConf(DictItemReq itemValueReq, User user);

    List<DictItemResp> queryDictConf(DictItemFilter dictItemFilter, User user);
}
