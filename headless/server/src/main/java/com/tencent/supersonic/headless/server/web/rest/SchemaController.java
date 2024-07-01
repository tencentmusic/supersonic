package com.tencent.supersonic.headless.server.web.rest;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.headless.api.pojo.response.DomainResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.server.web.service.SchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/api/semantic/schema")
public class SchemaController {

    @Autowired
    private SchemaService schemaService;

    @GetMapping("/domain/list")
    public List<DomainResp> getDomainList(HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return schemaService.getDomainList(user);
    }

    @GetMapping("/model/list")
    public List<ModelResp> getModelList(@RequestParam("domainId") Long domainId,
            @RequestParam("authType") String authType,
            HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return schemaService.getModelList(user, AuthType.valueOf(authType), domainId);
    }

}
