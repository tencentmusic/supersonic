package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.QueryViewReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.request.ViewReq;
import com.tencent.supersonic.headless.api.pojo.response.ViewResp;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;

import java.util.List;
import java.util.Map;

public interface ViewService {

    ViewResp save(ViewReq viewReq, User user);

    ViewResp update(ViewReq viewReq, User user);

    ViewResp getView(Long id);

    List<ViewResp> getViewList(MetaFilter metaFilter);

    void delete(Long id, User user);

    Map<Long, List<Long>> getModelIdToViewIds(List<Long> viewIds);

    List<ViewResp> getViews(User user);

    List<ViewResp> getViewsInheritAuth(User user, Long domainId);

    SemanticQueryReq convert(QueryViewReq queryViewReq);
}
