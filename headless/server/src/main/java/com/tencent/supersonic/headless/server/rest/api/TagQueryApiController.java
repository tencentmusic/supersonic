package com.tencent.supersonic.headless.server.rest.api;

import com.tencent.supersonic.headless.api.pojo.request.QueryTagReq;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/semantic/query")
@Slf4j
public class TagQueryApiController {

    @PostMapping("/tag")
    public Object queryByTag(@RequestBody QueryTagReq queryTagReq,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        //TODO
        return null;
    }

}
