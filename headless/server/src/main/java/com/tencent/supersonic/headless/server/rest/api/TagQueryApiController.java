package com.tencent.supersonic.headless.server.rest.api;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.headless.api.pojo.request.QueryTagReq;
import com.tencent.supersonic.headless.server.service.QueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/semantic/query")
@Slf4j
public class TagQueryApiController {

    @Autowired
    private QueryService queryService;

    @PostMapping("/tag")
    public Object queryByTag(@RequestBody QueryTagReq queryTagReq,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        return queryService.queryByReq(queryTagReq, user);
    }

}
