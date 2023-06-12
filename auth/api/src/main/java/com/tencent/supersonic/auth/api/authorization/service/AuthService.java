package com.tencent.supersonic.auth.api.authorization.service;

import com.tencent.supersonic.auth.api.authorization.request.QueryAuthResReq;
import com.tencent.supersonic.auth.api.authorization.response.AuthorizedResourceResp;
import javax.servlet.http.HttpServletRequest;

public interface AuthService {

    AuthorizedResourceResp queryAuthorizedResources(HttpServletRequest request, QueryAuthResReq req);
}
