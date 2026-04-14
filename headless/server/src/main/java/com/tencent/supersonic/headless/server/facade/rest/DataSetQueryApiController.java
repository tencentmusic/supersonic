package com.tencent.supersonic.headless.server.facade.rest;

import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.facade.service.SemanticLayerService;
import com.tencent.supersonic.headless.api.pojo.request.QueryDataSetReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.service.DataSetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/semantic/query")
@Slf4j
@RequiredArgsConstructor
public class DataSetQueryApiController {

    private final DataSetService dataSetService;

    private final SemanticLayerService semanticLayerService;

    @PostMapping("/dataSet")
    public Object queryByDataSet(@RequestBody QueryDataSetReq queryDataSetReq,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        SemanticQueryReq queryReq = dataSetService.convert(queryDataSetReq);
        return semanticLayerService.queryByReq(queryReq, user);
    }
}
