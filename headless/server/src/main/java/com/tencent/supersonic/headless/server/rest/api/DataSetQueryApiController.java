package com.tencent.supersonic.headless.server.rest.api;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.headless.api.pojo.request.QueryDataSetReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.server.service.DataSetService;
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
public class DataSetQueryApiController {

    @Autowired
    private DataSetService dataSetService;
    @Autowired
    private QueryService queryService;

    @PostMapping("/dataSet")
    public Object queryByDataSet(@RequestBody QueryDataSetReq queryDataSetReq,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        SemanticQueryReq queryReq = dataSetService.convert(queryDataSetReq);
        return queryService.queryByReq(queryReq, user);
    }

}
