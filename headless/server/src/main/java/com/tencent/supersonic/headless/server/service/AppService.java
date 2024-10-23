package com.tencent.supersonic.headless.server.service;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.AppQueryReq;
import com.tencent.supersonic.headless.api.pojo.request.AppReq;
import com.tencent.supersonic.headless.api.pojo.response.AppDetailResp;
import com.tencent.supersonic.headless.api.pojo.response.AppResp;

public interface AppService {

    AppDetailResp save(AppReq app, User user);

    AppDetailResp update(AppReq app, User user);

    void online(Integer id, User user);

    void offline(Integer id, User user);

    void delete(Integer id, User user);

    PageInfo<AppResp> pageApp(AppQueryReq appQueryReq, User user);

    AppDetailResp getApp(Integer id, User user);

    AppDetailResp getApp(Integer id);
}
