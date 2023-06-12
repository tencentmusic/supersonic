package com.tencent.supersonic.auth.authorization.application;

import com.tencent.supersonic.auth.api.authorization.request.QueryAuthResReq;
import com.tencent.supersonic.auth.api.authorization.response.AuthorizedResourceResp;
import com.tencent.supersonic.auth.api.authorization.service.AuthService;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthApplicationService authApplicationService;

    public AuthServiceImpl(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    @Override
    public AuthorizedResourceResp queryAuthorizedResources(HttpServletRequest request, QueryAuthResReq req) {
        return authApplicationService.queryAuthorizedResources(req, request);
    }
}
