package com.tencent.supersonic.headless.server.web.service;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.ItemValueReq;
import com.tencent.supersonic.headless.api.pojo.response.ItemValueResp;

public interface TagQueryService {

    ItemValueResp queryTagValue(ItemValueReq itemValueReq, User user) throws Exception;
}
