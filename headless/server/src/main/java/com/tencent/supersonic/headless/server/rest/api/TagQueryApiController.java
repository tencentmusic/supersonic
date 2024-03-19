package com.tencent.supersonic.headless.server.rest.api;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.server.service.QueryService;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/semantic/query")
@Slf4j
public class TagQueryApiController {

    @Autowired
    private QueryService queryService;

    @PostMapping("/tag")
    public Object queryByTag(@RequestBody QueryStructReq queryStructReq,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        return queryService.queryByReq(queryStructReq.convert(), user);
    }

}
